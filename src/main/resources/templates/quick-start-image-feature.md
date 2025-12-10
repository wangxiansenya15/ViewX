# ViewX 图片功能快速启动指南

## 🚀 5分钟快速启动

### 第一步: 数据库初始化

```bash
# 连接到 PostgreSQL 数据库
psql -U viewx_user -d viewx_db

# 执行建表脚本
\i src/main/resources/sql/contents.sql

# 验证表是否创建成功
\dt vx_contents

# 退出
\q
```

### 第二步: 启动后端

```bash
# 进入项目目录
cd /home/arthur/Desktop/ViewX

# 编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run
```

### 第三步: 启动前端

```bash
# 进入前端目录
cd ViewX-frontend

# 安装依赖 (如果还没安装)
npm install

# 启动开发服务器
npm run dev
```

### 第四步: 测试功能

打开浏览器访问: `http://localhost:5173`

1. **登录账号**
2. **访问上传页面** - 导航到图片上传页面
3. **上传图片** - 选择或拖拽图片
4. **查看效果** - 在个人主页查看上传的内容

## 📋 API 测试

### 测试单张图片上传

```bash
# 获取 Token (先登录)
TOKEN="your_jwt_token_here"

# 上传图片
curl -X POST http://localhost:8080/api/contents/image \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/your/image.jpg" \
  -F "title=测试图片" \
  -F "description=这是一张测试图片" \
  -F "category=测试" \
  -F "visibility=PUBLIC"
```

### 测试图片集上传

```bash
curl -X POST http://localhost:8080/api/contents/image-set \
  -H "Authorization: Bearer $TOKEN" \
  -F "files=@/path/to/image1.jpg" \
  -F "files=@/path/to/image2.jpg" \
  -F "files=@/path/to/image3.jpg" \
  -F "title=测试图片集" \
  -F "description=这是一个测试图片集" \
  -F "visibility=PUBLIC"
```

### 查询内容

```bash
# 获取内容详情
curl http://localhost:8080/api/contents/1

# 获取我的内容
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/contents/my

# 只获取图片
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/contents/my?type=IMAGE

# 只获取图片集
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/contents/my?type=IMAGE_SET
```

## 🔍 验证清单

- [ ] 数据库表 `vx_contents` 创建成功
- [ ] 后端服务启动成功 (端口 8080)
- [ ] 前端服务启动成功 (端口 5173)
- [ ] 可以成功登录
- [ ] 可以上传单张图片
- [ ] 可以上传图片集 (2-9张)
- [ ] 可以查看上传的内容
- [ ] 缩略图正常生成
- [ ] 可以删除内容

## 🐛 常见问题

### 1. 数据库连接失败
```bash
# 检查 PostgreSQL 是否运行
sudo systemctl status postgresql

# 启动 PostgreSQL
sudo systemctl start postgresql
```

### 2. 表已存在错误
```sql
-- 删除旧表 (谨慎操作!)
DROP TABLE IF EXISTS vx_contents CASCADE;

-- 重新创建
\i src/main/resources/sql/contents.sql
```

### 3. 文件上传失败
检查配置文件 `application-dev.yml`:
```yaml
file:
  storage:
    type: local
    local:
      uploadDir: /var/www/html/viewx
      baseUrl: http://localhost/viewx
```

确保目录存在且有写权限:
```bash
sudo mkdir -p /var/www/html/viewx/images
sudo mkdir -p /var/www/html/viewx/images/sets
sudo mkdir -p /var/www/html/viewx/images/thumbnails
sudo chown -R $USER:$USER /var/www/html/viewx
sudo chmod -R 755 /var/www/html/viewx
```

### 4. 前端编译错误
```bash
# 清除缓存
rm -rf node_modules package-lock.json

# 重新安装
npm install

# 重启开发服务器
npm run dev
```

## 📚 下一步

1. **阅读文档**
   - [使用指南](./image-upload-guide.md)
   - [API 文档](./image-upload-guide.md#api-接口文档)
   - [实现总结](./image-feature-summary.md)

2. **自定义开发**
   - 创建内容展示组件
   - 添加图片编辑功能
   - 实现图片搜索

3. **性能优化**
   - 配置 CDN
   - 启用图片压缩
   - 实现懒加载

## 💡 提示

- 图片最大 10MB
- 图片集支持 2-9 张图片
- 支持格式: JPG, JPEG, PNG, GIF, WEBP
- 自动生成缩略图 (320x180)

## 🎉 完成!

恭喜! 您已经成功启动了 ViewX 的图片上传功能。

现在可以开始上传和分享精美的图片了! 🎨📸
