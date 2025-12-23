# ViewX 项目 Docker 部署内存配置指南

## 🎯 推荐配置

### 最小配置（1.5 GB）- 仅用于开发测试
```yaml
# docker-compose.yml
version: '3.8'

services:
  backend:
    image: your-dockerhub-username/viewx-backend:latest
    container_name: viewx-backend
    environment:
      - JAVA_OPTS=-Xmx512m -Xms256m
    mem_limit: 768m
    mem_reservation: 512m
    
  postgres:
    image: postgres:15-alpine
    container_name: viewx-postgres
    environment:
      - POSTGRES_DB=viewx_db
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=your_password
    mem_limit: 384m
    mem_reservation: 256m
    command: >
      postgres
      -c shared_buffers=128MB
      -c effective_cache_size=256MB
    
  redis:
    image: redis:7-alpine
    container_name: viewx-redis
    command: redis-server --maxmemory 128mb --maxmemory-policy allkeys-lru
    mem_limit: 192m
    mem_reservation: 128m
    
  frontend:
    image: nginx:alpine
    container_name: viewx-frontend
    mem_limit: 128m
    mem_reservation: 64m
```

**总内存需求：~1.5 GB**

---

### 推荐配置（4 GB）- 小型生产环境 ⭐ 推荐
```yaml
# docker-compose.yml
version: '3.8'

services:
  backend:
    image: your-dockerhub-username/viewx-backend:latest
    container_name: viewx-backend
    restart: unless-stopped
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JAVA_OPTS=-Xmx1536m -Xms768m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
    mem_limit: 2g
    mem_reservation: 1g
    cpus: 2
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    
  postgres:
    image: postgres:15-alpine
    container_name: viewx-postgres
    restart: unless-stopped
    environment:
      - POSTGRES_DB=viewx_db
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=${DB_PASSWORD}
      - PGDATA=/var/lib/postgresql/data/pgdata
    volumes:
      - postgres_data:/var/lib/postgresql/data
    mem_limit: 1g
    mem_reservation: 512m
    cpus: 1
    command: >
      postgres
      -c shared_buffers=256MB
      -c effective_cache_size=512MB
      -c maintenance_work_mem=128MB
      -c checkpoint_completion_target=0.9
      -c wal_buffers=16MB
      -c default_statistics_target=100
      -c random_page_cost=1.1
      -c effective_io_concurrency=200
      -c work_mem=4MB
      -c min_wal_size=1GB
      -c max_wal_size=4GB
    
  redis:
    image: redis:7-alpine
    container_name: viewx-redis
    restart: unless-stopped
    command: >
      redis-server
      --maxmemory 512mb
      --maxmemory-policy allkeys-lru
      --save 900 1
      --save 300 10
      --save 60 10000
      --appendonly yes
    volumes:
      - redis_data:/data
    mem_limit: 640m
    mem_reservation: 512m
    cpus: 0.5
    
  frontend:
    image: nginx:alpine
    container_name: viewx-frontend
    restart: unless-stopped
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./dist:/usr/share/nginx/html:ro
    mem_limit: 256m
    mem_reservation: 128m
    cpus: 0.5

volumes:
  postgres_data:
  redis_data:

networks:
  default:
    name: viewx-network
```

**总内存需求：~4 GB**

---

### 标准配置（6 GB）- 中型生产环境
```yaml
# docker-compose.yml
version: '3.8'

services:
  backend:
    image: your-dockerhub-username/viewx-backend:latest
    container_name: viewx-backend
    restart: unless-stopped
    deploy:
      replicas: 2  # 双实例负载均衡
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JAVA_OPTS=-Xmx1536m -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError
    
  postgres:
    image: postgres:15-alpine
    container_name: viewx-postgres
    restart: unless-stopped
    environment:
      - POSTGRES_DB=viewx_db
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    mem_limit: 2g
    mem_reservation: 1g
    cpus: 2
    command: >
      postgres
      -c shared_buffers=512MB
      -c effective_cache_size=1GB
      -c maintenance_work_mem=256MB
      -c max_connections=200
    
  redis:
    image: redis:7-alpine
    container_name: viewx-redis
    restart: unless-stopped
    command: redis-server --maxmemory 1gb --maxmemory-policy allkeys-lru
    volumes:
      - redis_data:/data
    mem_limit: 1280m
    mem_reservation: 1g
    cpus: 1

volumes:
  postgres_data:
  redis_data:
```

**总内存需求：~6 GB**

---

## 📊 内存分配详解

### Spring Boot 应用

#### JVM 参数建议

**最小配置（512 MB）：**
```bash
JAVA_OPTS=-Xmx512m -Xms256m
```

**推荐配置（1.5 GB）：**
```bash
JAVA_OPTS=-Xmx1536m -Xms768m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

**标准配置（2 GB）：**
```bash
JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/heapdump.hprof
```

#### 内存分配说明

```
总内存 = 堆内存 + 非堆内存 + 直接内存 + 线程栈

示例（1.5 GB 配置）：
- 堆内存（-Xmx）：1536 MB
- 非堆内存（Metaspace）：~256 MB
- 直接内存（NIO）：~128 MB
- 线程栈（200 线程 × 1MB）：~200 MB
- 其他（GC、JIT）：~100 MB
-----------------------------------
总计：~2.2 GB（容器限制 2 GB，略紧张）
```

### PostgreSQL

#### 内存参数建议

**最小配置（256 MB）：**
```ini
shared_buffers = 64MB
effective_cache_size = 128MB
work_mem = 2MB
```

**推荐配置（512 MB）：**
```ini
shared_buffers = 256MB
effective_cache_size = 512MB
work_mem = 4MB
maintenance_work_mem = 128MB
```

**标准配置（1 GB）：**
```ini
shared_buffers = 512MB
effective_cache_size = 1GB
work_mem = 8MB
maintenance_work_mem = 256MB
max_connections = 200
```

### Redis

#### 内存策略

**最小配置（128 MB）：**
```bash
redis-server --maxmemory 128mb --maxmemory-policy allkeys-lru
```

**推荐配置（512 MB）：**
```bash
redis-server --maxmemory 512mb --maxmemory-policy allkeys-lru
```

**标准配置（1 GB）：**
```bash
redis-server --maxmemory 1gb --maxmemory-policy allkeys-lru
```

---

## 🎯 根据并发量选择配置

| 并发用户 | 推荐内存 | 配置方案 | 说明 |
|---------|---------|---------|------|
| < 100 | 2 GB | 最小配置 | 开发测试 |
| 100-500 | 4 GB | 推荐配置 ⭐ | 小型生产 |
| 500-2000 | 6 GB | 标准配置 | 中型生产 |
| 2000-5000 | 10 GB | 高性能配置 | 大型生产 |
| > 5000 | 16 GB+ | 集群部署 | 需要横向扩展 |

---

## ⚠️ 注意事项

### 1. 容器内存限制

```yaml
services:
  backend:
    mem_limit: 2g          # 硬限制（不能超过）
    mem_reservation: 1g    # 软限制（保证分配）
```

- `mem_limit`：容器最大可用内存，超过会被 OOM Kill
- `mem_reservation`：保证分配的内存，系统会优先保证

### 2. JVM 堆内存设置

⚠️ **重要**：JVM 堆内存应该小于容器限制

```
容器限制 = JVM 堆内存 + 非堆内存 + 其他

推荐比例：
- 容器限制 2 GB → JVM 堆 1.5 GB
- 容器限制 4 GB → JVM 堆 3 GB
- 容器限制 8 GB → JVM 堆 6 GB
```

### 3. 监控内存使用

```bash
# 查看容器内存使用
docker stats

# 查看 JVM 内存使用
docker exec viewx-backend jmap -heap 1

# 查看 PostgreSQL 内存使用
docker exec viewx-postgres psql -U postgres -c "SHOW shared_buffers;"
```

---

## 📈 性能优化建议

### 1. 启用 JVM 参数优化

```bash
JAVA_OPTS="
  -Xmx1536m
  -Xms768m
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/logs/heapdump.hprof
  -XX:+PrintGCDetails
  -XX:+PrintGCDateStamps
  -Xloggc:/logs/gc.log
  -XX:+UseGCLogFileRotation
  -XX:NumberOfGCLogFiles=10
  -XX:GCLogFileSize=10M
"
```

### 2. PostgreSQL 连接池配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 3. Redis 连接池配置

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: 3000
```

---

## 🚀 部署建议

### 云服务器推荐配置

| 云厂商 | 实例类型 | vCPU | 内存 | 价格/月 | 适用场景 |
|--------|---------|------|------|---------|---------|
| 阿里云 | ecs.t6-c1m2.large | 2 | 4 GB | ~¥200 | 小型生产 ⭐ |
| 腾讯云 | S5.MEDIUM4 | 2 | 4 GB | ~¥180 | 小型生产 |
| AWS | t3.medium | 2 | 4 GB | ~$30 | 小型生产 |
| 阿里云 | ecs.c6.xlarge | 4 | 8 GB | ~¥400 | 中型生产 |

---

## 📝 总结

**最终推荐：**

- **开发/测试**：2 GB 内存服务器
- **小型生产**：4 GB 内存服务器 ⭐ **推荐**
- **中型生产**：6-8 GB 内存服务器
- **大型生产**：16 GB+ 或集群部署

**理由：**
1. 您的项目包含 AI 功能、视频处理、实时聊天
2. 使用了 pgvector、GIN 索引等高级特性
3. 需要足够的缓存空间保证性能
4. 4 GB 是性价比最高的选择
