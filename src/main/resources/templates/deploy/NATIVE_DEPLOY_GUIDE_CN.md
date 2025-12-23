# ViewX 低内存原生部署指南 (Native Low-Memory Deployment Guide)

本指南针对两台低内存 Linux 服务器设计，旨在最大程度节省资源并保证服务稳定性。

## 🖥️ 服务器资源规划

| 服务器 | IP (示例) | 角色 | 部署组件 | 内存策略 |
| :--- | :--- | :--- | :--- | :--- |
| **Server A (Main)** | 192.168.1.10 | 应用/Web | Nginx, Java (Spring Boot) | 限制 Java Heap，利用 Swap |
| **Server B (Data)** | 192.168.1.11 | 数据存储 | PostgreSQL, Redis, RabbitMQ | 严格限制缓存大小，利用 Swap |

---

## 🛠️ 第一步：环境准备 (两台通用)

1.  **创建运行用户** (安全起见，不建议用 root 运行服务):
    ```bash
    useradd -m -s /bin/bash viewx
    mkdir -p /var/www/viewx
    chown -R viewx:viewx /var/www/viewx
    ```

2.  **配置 Swap (虚拟内存)**:
    由于物理内存紧张，Swap 是防线。请确保 Swap 启用 (Server A 已有 2G，建议 check Server B)。
    ```bash
    free -h
    # 如果没有 Swap，创建 2G Swap 文件
    fallocate -l 2G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
    ```

---

## 🚀 第二步：Server A (主服务器) 部署

### 1. 安装 JDK 17
```bash
# Ubuntu/Debian
apt update && apt install openjdk-17-jdk -y

# CentOS/RHEL
yum install java-17-openjdk -y
```

### 2. 构建与部署应用
在你的开发机 (Windows) 上构建 Jar 包和前端资源：

```powershell
# 后端构建 (生成的 jar 在 target/ 目录)
./mvnw clean package -DskipTests

# 前端构建 (生成的资源在 ViewX-frontend/dist/ 目录)
cd ViewX-frontend
npm install
npm run build
```

**上传文件到 Server A:**
将 `viewx-backend.jar` 上传到 `/var/www/viewx/`。
将 `dist` 目录内容上传到 `/var/www/viewx/frontend/`。

### 3. 配置 Systemd 服务 (后端)
使用我们准备好的 `deploy/viewx.service` 文件。

```bash
# 复制服务文件 (需要 root 权限)
cp deploy/viewx.service /etc/systemd/system/viewx.service
systemctl daemon-reload
systemctl enable viewx
systemctl start viewx
```
*注意：该服务文件限制了 `-Xmx512m`，这是关键配置。*

### 4. 安装与配置 Nginx (前端 + 反向代理)
```bash
# Ubuntu
apt install nginx -y
```

使用 `deploy/viewx.nginx.conf` 配置文件：
```bash
cp deploy/viewx.nginx.conf /etc/nginx/sites-available/viewx
ln -s /etc/nginx/sites-available/viewx /etc/nginx/sites-enabled/
rm /etc/nginx/sites-enabled/default  # 移除默认配置
nginx -t
systemctl restart nginx
```

---

## 💾 第三步：Server B (数据服务器) 部署

由于这台机器只有 1.4G 内存要跑三个服务，必须手动修改配置文件。

### 1. PostgreSQL 安装与调优
```bash
# Ubuntu 安装
apt install postgresql postgresql-contrib -y
```

**关键调优 (`/etc/postgresql/{version}/main/postgresql.conf`)**:
找到并修改以下参数，以极低内存运行：
```conf
shared_buffers = 256MB   # 默认通常是 128MB，给 256MB 足够小规模使用
work_mem = 4MB           # 每个查询操作的内存，保持小
maintenance_work_mem = 64MB
effective_cache_size = 512MB
max_connections = 50     # 可以在应用层控制连接池大小，这里不要太大
```
*重启 PG:* `systemctl restart postgresql`

**创建库与用户:**
```bash
sudo -u postgres psql
postgres=# CREATE DATABASE viewx_db;
postgres=# CREATE USER viewx_user WITH ENCRYPTED PASSWORD 'your_secure_password';
postgres=# GRANT ALL PRIVILEGES ON DATABASE viewx_db TO viewx_user;
```

### 2. Redis 安装与调优
```bash
apt install redis-server -y
```

**关键调优 (`/etc/redis/redis.conf`)**:
```conf
maxmemory 256mb          # 强制限制最大内存，防止 OOM 杀进程
maxmemory-policy allkeys-lru # 内存满时淘汰最近最少使用的 key
```
*重启 Redis:* `systemctl restart redis-server`

### 3. RabbitMQ 安装与调优
RabbitMQ (Erlang) 比较吃内存，是最不确定的因素。

```bash
apt install rabbitmq-server -y
```

**启用管理插件 (可选，约消耗 50MB 内存)**:
```bash
rabbitmq-plugins enable rabbitmq_management
```

**内存限制 (创建 `/etc/rabbitmq/rabbitmq.config`)**:
如果不限制，Erlang 可能会吃掉所有内存。
```erlang
[
  {rabbit, [
    {vm_memory_high_watermark, 0.4} 
  ]}
].
```
*解读*: 0.4 表示使用系统物理内存的 40% (1.4G * 0.4 ≈ 560MB) 作为高水位线，超过会阻塞发布者。

**创建用户:**
```bash
rabbitmqctl add_user admin your_secure_password
rabbitmqctl set_user_tags admin administrator
rabbitmqctl set_permissions -p / admin ".*" ".*" ".*"
```

---

## 🔗 第四步：修改应用配置

在 Server A 上，编辑 `application-prod.yml` (或者通过环境变量覆盖)，指向 Server B 的 IP。

```yaml
spring:
  datasource:
    url: jdbc:postgresql://192.168.1.11:5432/viewx_db
  redis:
    host: 192.168.1.11
  rabbitmq:
    host: 192.168.1.11
```

## ⚠️ 维护与监控
1.  **Swap 使用率**: 这种低内存配置下，Swap 会被频繁使用，服务稍微慢一点是正常的。
2.  **OOM Killer**: 如果发现进程突然消失，查看 `dmesg | grep -i kill`。
3.  **日志清理**: 配置 Logrotate 防止日志占满磁盘。
