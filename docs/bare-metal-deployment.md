# 极致轻量部署方案 (裸机部署 - No Docker)

本方案旨在为两台 2GB 内存服务器挤出每一 MB 的性能。通过去除 Docker 虚拟化开销，直接在宿主机运行服务。

## 🏛️ 架构规划（优化版）

- **Server A (192.168.1.100)**: 应用节点，计算与热数据
  - Spring Boot (1.0GB 堆内存)
  - Redis (300MB 本地缓存，极速访问) ⭐ 变更
  - Nginx (反向代理)
  
- **Server B (192.168.1.101)**: 数据持久化节点
  - PostgreSQL (独占 1.5GB 内存，性能提升) ⭐ 变更

---

## 💾 Server B: 纯数据库节点

### 1. PostgreSQL 15 优化安装

**安装:**
```bash
sudo apt update && sudo apt install -y postgresql-15
```

**配置 (`/etc/postgresql/15/main/postgresql.conf`):**
```ini
# 允许监听的 IP 地址
# 方式 1: 监听所有 IP
# listen_addresses = '*'

# 方式 2: 指定本机 IP (更安全，推荐)
listen_addresses = 'localhost,192.168.1.101'

# 内存优化 (Server B 独占数据库，可分配更多内存)
shared_buffers = 768MB        # 提升到了 768MB
effective_cache_size = 1536MB # 告诉 PG 有更多 OS 缓存可用
maintenance_work_mem = 128MB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
work_mem = 8MB                # 增加排序内存
min_wal_size = 1GB
max_wal_size = 4GB
```

**权限 (`/etc/postgresql/15/main/pg_hba.conf`):**
```bash
# 允许 Server A 连接
host    all             all             192.168.1.100/32        scram-sha-256
```

**重启:**
```bash
sudo systemctl restart postgresql
```

---

## 🖥️ Server A: 全栈应用节点

### 1. 部署 Redis (本地极速缓存)

**安装:**
```bash
sudo apt install -y redis-server
```

**配置 (`/etc/redis/redis.conf`):**
```ini
# 仅绑定本机 (更安全，因为只给本机的 Spring Boot 用)
bind 127.0.0.1 

# 内存限制 (严格限制，给 JVM 留空间)
maxmemory 256mb
maxmemory-policy allkeys-lru

# 关闭 RDB (节省 Server A IO，靠 AOF 即可)
save ""
appendonly yes
```

**重启:**
```bash
sudo systemctl restart redis-server
```

### 2. 部署后端 (Spring Boot)

**安装 JDK:**
```bash
sudo apt install -y openjdk-17-jdk
# 验证
java -version
```

**创建系统服务:**
创建文件 `/etc/systemd/system/viewx.service`:

```ini
[Unit]
Description=ViewX Backend
After=syslog.target network.target redis-server.service

[Service]
User=www-data
WorkingDirectory=/var/www/viewx
# 启动命令 (堆内存下调至 1000m，给 Redis 腾地)
ExecStart=/usr/bin/java \
    -Xms1000m -Xmx1000m \
    -XX:+UseSerialGC \
    -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m \
    -Xss512k \
    -Dspring.profiles.active=prod \
    -Dspring.datasource.url=jdbc:postgresql://192.168.1.101:5432/viewx_db \
    -Dspring.datasource.password=your_db_password \
    -Dspring.redis.host=127.0.0.1 \
    -Dspring.redis.password=your_redis_password \
    -jar viewx-backend.jar


SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**参数解读:**
- `-Xmx1200m`: 没有了 Docker 开销，我们可以大胆给 JVM 1.2GB 内存。
- `-XX:+UseSerialGC`: 单线程 GC，虽然只有 2GB 内存，但 CPU 可能也不强，SerialGC 开销最小，且没有 G1 的复杂内存结构，最省内存。

**启动服务:**
```bash
sudo systemctl daemon-reload
sudo systemctl enable viewx
sudo systemctl start viewx
```

### 2. 部署前端 (Nginx)

**安装:**
```bash
sudo apt install -y nginx
```

**配置 (`/etc/nginx/sites-available/viewx`):**

```nginx
server {
    listen 80;
    server_name your_domain.com;

    # 开启 Gzip 压缩 (拿 CPU 换带宽，值得)
    gzip on;
    gzip_min_length 1k;
    gzip_comp_level 6;
    gzip_types text/plain application/json application/javascript text/css;

    # 前端静态资源
    location / {
        root /var/www/viewx-frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
        
        # 静态资源缓存 30 天
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
            expires 30d;
            add_header Cache-Control "public, no-transform";
        }
    }

    # 后端 API 反向代理
    location /api {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # WebSocket 支持 (聊天功能必需)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

启用配置:
```bash
sudo ln -s /etc/nginx/sites-available/viewx /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

## 🛠️ 运维与监控

### 1. 必备: 开启 Swap (两台都要)
即使节省了 Docker 内存，2GB 依然危险。必须开 Swap 兜底。

```bash
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
# 只有当内存剩余 < 40% 时才用 swap
sudo sysctl vm.swappiness=40
```

### 2. 部署脚本 (deploy.sh)
虽然没有 Docker 方便，但我们可以写个简单的 Shell 脚本来自动化更新。

```bash
#!/bin/bash
# 简单的裸机部署脚本

echo "停止服务..."
sudo systemctl stop viewx

echo "备份旧 Jar..."
mv /var/www/viewx/viewx-backend.jar /var/www/viewx/viewx-backend.jar.bak

echo "部署新 Jar..."
cp target/viewx-backend.jar /var/www/viewx/

echo "重启服务..."
sudo systemctl start viewx

echo "部署完成！"
```
