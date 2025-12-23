# Cursor Rules 配置说明

## 📝 关于 .cursorrules

`.cursorrules` 是 Cursor AI 编辑器的项目规则配置文件，用于定义项目的编码规范、架构约定和 AI 辅助行为。

## 🚀 快速开始

### 首次使用

1. 复制示例文件：
   ```bash
   cp .cursorrules.example .cursorrules
   ```

2. 根据您的需要修改 `.cursorrules` 文件

3. 重启 Cursor 编辑器使配置生效

### 团队协作

`.cursorrules` 文件已添加到 `.gitignore`，因为：
- ✅ 每个开发者可能有不同的个人偏好
- ✅ 避免团队成员之间的配置冲突
- ✅ 可以根据本地环境自定义规则

## 📋 当前规则说明

### 1. ViewX 后端核心架构
- 使用 Java 17+ 和 Spring Boot 3
- 严格遵循 Controller/Service/DAO 分层
- DTO（入参）、VO（出参）、Entity（数据库）

### 2. ViewX 统一返回规范
- 所有 Controller 必须使用 `Result` 作为返回类型
- 成功：`Result.success()`、`Result.success(data)`
- 失败：`Result.badRequest()`、`Result.unauthorized()` 等
- 禁止使用 `ResponseEntity<Map<String, Object>>`

### 3. ViewX Servlet API 版本
- Spring Boot 3.x 使用 `jakarta.servlet`
- 不使用 `javax.servlet`
- 导入示例：`import jakarta.servlet.http.HttpServletRequest;`

### 4. ViewX 高性能组件与 AI
- Redis：高性能计数和缓存
- RabbitMQ：异步任务解耦
- PostgreSQL pgvector：AI 语义搜索

### 5. 回复语言偏好
- AI 回复使用中文（简体）

## 🔧 自定义规则

您可以根据需要添加或修改规则：

```json
{
  "rules": [
    {
      "name": "您的规则名称",
      "description": "规则描述",
      "match": "**/path/to/files/**",
      "context": [
        "规则内容1",
        "规则内容2"
      ]
    }
  ]
}
```

## 📚 更多信息

- [Cursor 官方文档](https://cursor.sh/docs)
- ViewX 项目文档：`docs/` 目录

## ⚠️ 注意事项

1. **不要提交 `.cursorrules` 到 Git**
   - 该文件已在 `.gitignore` 中
   - 只提交 `.cursorrules.example`

2. **更新示例文件**
   - 如果添加了有用的规则，请更新 `.cursorrules.example`
   - 方便其他团队成员参考

3. **保持同步**
   - 定期检查 `.cursorrules.example` 的更新
   - 将有用的规则合并到您的 `.cursorrules`

## 🎯 最佳实践

### 推荐的个人配置

```json
{
  "rules": [
    // 复制 .cursorrules.example 中的所有规则
    
    // 添加您的个人偏好规则
    {
      "name": "个人代码风格",
      "context": [
        "您的个人偏好..."
      ]
    }
  ]
}
```

### 笔记本和主机协同

如果您在多台设备上工作：

1. **方案一：使用云同步**
   ```bash
   # 将 .cursorrules 放在云盘目录
   ln -s ~/Dropbox/.cursorrules .cursorrules
   ```

2. **方案二：使用 Git 私有分支**
   ```bash
   # 创建个人配置分支（不推送到远程）
   git checkout -b personal-config
   git add .cursorrules
   git commit -m "Personal cursor rules"
   ```

3. **方案三：环境变量**
   - 根据不同设备使用不同的配置
   - 在 `.cursorrules` 中使用条件判断

## 📞 需要帮助？

如果您对规则配置有疑问：
1. 查看 `.cursorrules.example` 中的示例
2. 阅读项目文档
3. 咨询团队成员

---

**记住：`.cursorrules` 是您的个人配置，`.cursorrules.example` 是团队共享的模板！**
