# 配置管理系统使用指南

## 🎯 问题背景

部署到生产环境后，某些配置会过期（如QQ邮箱授权码），需要动态修改配置而不重新打包部署。

**传统方案的问题**:
- ❌ 修改 `application-prod.yml` → 需要重新打包
- ❌ 使用 Nacos → 太重，增加运维复杂度
- ❌ 手动修改 `.env` → 容易出错，没有备份

**我们的方案**:
- ✅ 通过管理员接口动态修改 `.env` 文件
- ✅ 自动备份配置
- ✅ **支持一键重启应用**
- ✅ 轻量级，无需额外组件

---

## 💻 前端管理面板

我们提供了一个图形化管理界面，路径：`/admin/config`

> 注意：此页面仅限管理员访问

### 功能列表

1. **查看可编辑项**: 系统仅列出允许修改的配置项（如邮箱配置、API Key）。
2. **安全编辑**: 出于安全考虑，**所有配置值均不回显**，编辑时需要重新输入完整值。
3. **快速更新邮箱授权码**: 针对最常过期的配置提供快捷入口。
4. **备份配置**: 手动备份当前 `.env` 文件。
5. **重启应用**: 修改配置后，点击 "🔄 重启应用" 按钮，系统将在 3 秒后重启。

---

## 🛡️ 安全限制

为了保护系统安全，配置管理服务已实施以下限制：

1. **白名单机制**: 仅允许修改以下配置项：
   - `MAIL_USERNAME`
   - `MAIL_PASSWORD`
   - `GITHUB_CLIENT_ID`
   - `GITHUB_CLIENT_SECRET`
   - `AI_API_KEY`
2. **盲写模式**: API 不提供获取配置值的接口，前端也无法查看当前配置。所有更新均为覆盖操作。

---

## 📋 命令行使用方式

### 1. 更新邮箱授权码（最常用）

```bash
curl -X PUT http://localhost:8080/api/admin/config/mail-password \
  -H "Authorization: Bearer <管理员Token>" \
  -H "Content-Type: application/json" \
  -d '{
    "password": "新的授权码"
  }'
```

### 2. 通过 API 重启应用

```bash
curl -X POST http://localhost:8080/api/admin/config/restart \
  -H "Authorization: Bearer <管理员Token>"
```

---

## 🔄 重启机制详解

后端实现了智能重启策略，会依次尝试以下方式：

1. **Docker Compose**: 尝试执行 `docker-compose restart viewx-backend`
2. **Systemd**: 尝试执行 `systemctl restart viewx`
3. **退出进程**: 如果上述方式都失败，将调用 `System.exit(0)`，依赖外部进程管理器（如 Docker 的 restart policy 或 Systemd）自动拉起服务。

**注意**: 重启过程会导致服务短暂不可用（约 10-30 秒）。

---

## 🛡️ 安全性

### 1. 权限控制

接口使用 `@PreAuthorize("hasRole('ADMIN')")` 保护，只有管理员可以访问。

### 2. 敏感信息脱敏

前端界面和日志中自动对包含 `PASSWORD`, `SECRET`, `KEY` 的配置项进行脱敏。

### 3. 自动备份

每次修改前自动备份当前配置到 `.env.backup.<timestamp>`。

---

**创建时间**: 2025-12-24  
**版本**: v1.1
