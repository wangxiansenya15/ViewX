# 双机部署方案 (2GB x 2)

本方案专为两台 2GB 内存的服务器设计，已优化 JVM 和数据库参数以防止内存溢出。

## 🖥️ 服务器角色分配

| 服务器 | 角色 | 部署组件 | 内存分配 | 说明 |
|-------|------|---------|---------|------|
| **Server A** | **应用节点** | Spring Boot, Nginx | 1.2 GB | CPU 密集型，负责业务逻辑 |
| **Server B** | **数据节点** | PostgreSQL, Redis | 1.0 GB | IO 密集型，利用 OS 缓存加速 |

---

## 🛠️ 第一步：系统级优化（两台都要做）

**非常重要**：2GB 内存很紧张，必须开启 **Swap (虚拟内存)**，防止突发流量导致进程被杀。

```bash
# 1. 创建 4GB 的 Swap 文件
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# 2. 永久生效
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 3. 调整 Swappiness (倾向于使用 Swap，保护物理内存)
sudo sysctl vm.swappiness=60
```

---

## 📦 Server A 配置 (应用节点)

负责运行后端和前端。

### `docker-compose.yml`

```yaml
version: '3.8'

services:
  # 1. 前端 Nginx
  frontend:
    image: nginx:alpine
    container_name: viewx-frontend
    restart: always
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./dist:/usr/share/nginx/html:ro
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
    mem_limit: 128m  # 限制 128MB，实际占用约 30-50MB

  # 2. 后端 Spring Boot
  backend:
    image: your-dockerhub-username/viewx-backend:latest
    container_name: viewx-backend
    restart: always
    ports:
      - "8080:8080"
    environment:
      # ⚠️ 连接 Server B 的 IP 地址
      - CHECK_USERNAME_URL=jdbc:postgresql://192.168.1.101:5432/viewx_db
      - SPRING_DATASOURCE_URL=jdbc:postgresql://192.168.1.101:5432/viewx_db
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=your_secure_password
      - SPRING_DATA_REDIS_HOST=192.168.1.101
      - SPRING_DATA_REDIS_PORT=6379
      # JVM 内存优化：堆内存 800MB，总占用约 1.1GB
      - JAVA_OPTS=-Xms512m -Xmx800m -XX:+UseSerialGC -Xss512k -XX:MaxDirectMemorySize=128m
    depends_on:
      - frontend
    mem_limit: 1200m # 限制 1.2GB
```

**优化点说明：**
- `-XX:+UseSerialGC`：单核/低内存下比 G1GC 更省内存。
- `-Xmx800m`：给 JVM 堆 800MB，加上非堆内存，容器总占用刚好控制在 1.1 - 1.2GB。
- `mem_limit`：防止 Java 吃光 Server A 的所有内存导致 SSH 连不上。

---

## 💾 Server B 配置 (数据节点)

负责运行数据库和缓存。假设内网 IP 为 `192.168.1.101`。

### `docker-compose.yml`

```yaml
version: '3.8'

services:
  # 1. PostgreSQL 数据库
  postgres:
    image: postgres:15-alpine
    container_name: viewx-postgres
    restart: always
    ports:
      - "5432:5432" # 暴露端口给 Server A 连接
    environment:
      - POSTGRES_DB=viewx_db
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=your_secure_password
      - PGDATA=/var/lib/postgresql/data/pgdata
    volumes:
      - postgres_data:/var/lib/postgresql/data
    command: >
      postgres
      -c shared_buffers=256MB
      -c work_mem=4MB
      -c maintenance_work_mem=64MB
      -c effective_cache_size=768MB
      -c max_connections=100
    mem_limit: 1g # 限制 1GB

  # 2. Redis 缓存
  redis:
    image: redis:7-alpine
    container_name: viewx-redis
    restart: always
    ports:
      - "6379:6379" # 暴露端口给 Server A 连接
    volumes:
      - redis_data:/data
    # 严格限制最大内存 256MB，启用 LRU 淘汰策略
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru --appendonly yes
    mem_limit: 384m

volumes:
  postgres_data:
  redis_data:
```

**优化点说明：**
- `shared_buffers=256MB`：PG 占用约 300MB 内存，剩余内存留给操作系统做文件缓存（Page Cache），这对数据库性能至关重要。
- `redis --maxmemory 256mb`：配合我们在代码里做的"只缓存热点数据"策略，256MB 绰绰有余。

---

## 🔗 网络连接设置

由于两台服务器是分开的，必须确保它们能通过网络通信。

1.  **内网 IP (推荐)**：
    如果两台服务器在同一个云服务商的同一账号下（如阿里云 VPC），使用内网 IP (如 `172.x.x.x` 或 `192.168.x.x`)。**速度快且免费**。
    
2.  **公网 IP (备选)**：
    如果不在同一内网，必须使用公网 IP。
    **安全警告**：如果是公网连接，**必须**配置防火墙！

    **Server B 防火墙规则 (仅允许 Server A 访问)：**
    ```bash
    # 假设 Server A 的 IP 是 123.123.123.123
    sudo ufw allow from 123.123.123.123 to any port 5432 # 放行 PG
    sudo ufw allow from 123.123.123.123 to any port 6379 # 放行 Redis
    sudo ufw enable
    ```

    同时，Redis 必须设置密码（默认无密码），PostgreSQL 密码要设得复杂。

## 📊 内存账单

**Server A (2GB):**
- 系统内核 + 进程: ~400MB
- Spring Boot: ~1100MB
- Nginx: ~50MB
- **剩余**: ~450MB (安全)

**Server B (2GB):**
- 系统内核 + 进程: ~400MB
- PostgreSQL: ~300MB (应用层) + OS Cache (动态占用剩余)
- Redis: ~300MB
- **剩余**: ~1GB (将被 OS 用作数据库的文件缓存，提升查询速度)

## 🚀 部署步骤

1.  **Server B**: 上传 `docker-compose.yml`，执行 `docker-compose up -d` 启动数据服务。
2.  **Server A**: 修改 `docker-compose.yml` 中的 IP 地址为 Server B 的真实 IP。
3.  **Server A**: 执行 `docker-compose up -d` 启动应用。
4.  **初始化**: 在 Server A 上进入容器执行数据库初始化：
    ```bash
    docker exec -it viewx-backend sh
    # 如果已配置自动运行 SQL 则无需此步
    ```
