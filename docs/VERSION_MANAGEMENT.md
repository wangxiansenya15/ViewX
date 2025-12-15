# ViewX 版本管理与升级指南

## 概述

ViewX 项目已集成完整的版本管理和自动升级系统，支持：
- ✅ 自动检测新版本
- ✅ 手动检查更新
- ✅ 版本迁移逻辑
- ✅ 强制/可选更新
- ✅ 更新日志展示

## 架构说明

### 后端 (Spring Boot)

#### 1. 核心类

**SystemVersionDTO** (`pojo/dto/SystemVersionDTO.java`)
- 版本信息数据传输对象
- 包含版本号、构建时间、更新日志等

**SystemVersionService** (`service/SystemVersionService.java`)
- 版本管理服务接口
- 定义版本检查和升级方法

**SystemVersionServiceImpl** (`service/impl/SystemVersionServiceImpl.java`)
- 版本服务实现
- 包含版本比较、升级逻辑执行

**SystemVersionController** (`controller/SystemVersionController.java`)
- REST API 控制器
- 提供版本查询、检查更新、执行升级接口

#### 2. API 接口

```
GET  /api/system/version          # 获取当前版本信息
GET  /api/system/check-update     # 检查是否有更新
POST /api/system/upgrade           # 执行版本升级
GET  /api/system/health            # 健康检查
```

#### 3. 配置

在 `application.yml` 中配置版本信息：

```yaml
app:
  version: 1.0.0
  build-time: 2025-12-15 14:48:00
```

### 前端 (Vue 3 + TypeScript)

#### 1. 核心文件

**system.ts** (`api/system.ts`)
- 系统 API 接口定义
- 包含版本检查相关的 API 调用

**versionChecker.ts** (`utils/versionChecker.ts`)
- 版本检查器工具类
- 支持自动和手动检查
- 处理更新通知和升级流程

**Settings.vue** (`views/Settings.vue`)
- 设置页面
- 集成手动检查更新功能

**App.vue**
- 应用入口
- 初始化自动版本检查

#### 2. 环境变量

在 `.env` 文件中配置：

```bash
VITE_APP_VERSION=1.0.0
VITE_BUILD_TIME=2025-12-15 14:48:00
```

## 使用指南

### 1. 发布新版本

#### 步骤 1: 更新版本号

**后端:**
```yaml
# application.yml
app:
  version: 1.0.1  # 更新版本号
  build-time: 2025-12-16 10:00:00  # 更新构建时间
```

**前端:**
```bash
# .env
VITE_APP_VERSION=1.0.1
VITE_BUILD_TIME=2025-12-16 10:00:00
```

```json
// package.json
{
  "version": "1.0.1"
}
```

#### 步骤 2: 添加升级逻辑（如需要）

在 `SystemVersionServiceImpl.java` 中添加版本升级逻辑：

```java
private void upgradeFrom1_0_1() {
    log.info("执行 1.0.1 版本升级逻辑");
    // 数据库迁移
    // 数据清理
    // 新功能初始化
}
```

并在 `performUpgrade` 方法中注册：

```java
if (needsUpgrade(fromVersion, "1.0.1", toVersion)) {
    upgradeFrom1_0_1();
}
```

#### 步骤 3: 更新更新日志

在 `SystemVersionServiceImpl.java` 的 `getUpdateLog` 方法中添加：

```java
case "1.0.1":
    return "- 修复了若干bug\n- 优化了性能\n- 新增了XX功能";
```

#### 步骤 4: 构建和部署

```bash
# 后端
cd ViewX
mvn clean package

# 前端
cd ViewX-frontend
npm run build

# 部署到服务器
```

### 2. 版本检查机制

#### 自动检查
- 应用启动后自动初始化版本检查器
- 每 5 分钟检查一次（可配置）
- 检测到新版本时自动弹出通知

#### 手动检查
- 用户在设置页面点击"检查更新"
- 立即查询服务器版本信息
- 显示详细的更新信息

#### 构建哈希检测
- 定期检查 `index.html` 中的 JS 文件哈希
- 检测到变化时提示用户刷新
- 适用于前端静态资源更新

### 3. 更新类型

**MAJOR (主版本)**
- 重大架构变更
- 不兼容的 API 修改
- 通常需要强制更新

**MINOR (次版本)**
- 新功能添加
- 向后兼容的改进
- 可选更新

**PATCH (补丁)**
- Bug 修复
- 性能优化
- 建议更新

### 4. 强制更新策略

在 `SystemVersionServiceImpl.java` 中配置：

```java
// 跨主版本必须强制更新
updateInfo.setForceUpdate(isMajorVersionChange(clientVersion, currentVersion));
```

## 版本迁移示例

### 示例 1: 数据库字段添加

```java
private void upgradeFrom1_0_0() {
    log.info("执行 1.0.0 -> 1.1.0 升级");
    
    // 使用 MyBatis-Plus 或原生 SQL
    // 添加新字段
    jdbcTemplate.execute(
        "ALTER TABLE user ADD COLUMN new_field VARCHAR(255)"
    );
    
    // 数据迁移
    userMapper.updateAllUsersWithDefaultValue();
}
```

### 示例 2: 配置项迁移

```java
private void upgradeFrom1_1_0() {
    log.info("执行 1.1.0 -> 1.2.0 升级");
    
    // 迁移旧配置到新配置
    String oldConfig = configService.get("old.config.key");
    configService.set("new.config.key", transformConfig(oldConfig));
    configService.remove("old.config.key");
}
```

### 示例 3: 缓存清理

```java
private void upgradeFrom2_0_0() {
    log.info("执行 2.0.0 -> 2.1.0 升级");
    
    // 清理旧版本缓存
    redisTemplate.delete("old:cache:*");
    
    // 初始化新缓存结构
    cacheService.initializeNewCacheStructure();
}
```

## 最佳实践

### 1. 版本号规范

遵循语义化版本 (Semantic Versioning):
- `MAJOR.MINOR.PATCH`
- 例如: `1.2.3`

### 2. 升级逻辑原则

- ✅ 幂等性：多次执行结果一致
- ✅ 可回滚：保留数据备份
- ✅ 向后兼容：尽量不破坏现有功能
- ✅ 日志记录：详细记录升级过程

### 3. 测试流程

```bash
# 1. 在开发环境测试升级
# 2. 在测试环境验证完整流程
# 3. 准备回滚方案
# 4. 生产环境发布
# 5. 监控升级状态
```

### 4. 回滚策略

```java
// 保留回滚方法
private void rollbackFrom1_1_0() {
    log.warn("回滚 1.1.0 版本升级");
    // 执行回滚逻辑
}
```

## 故障排查

### 问题 1: 版本检查失败

**症状**: 前端无法获取版本信息

**解决**:
1. 检查后端服务是否正常运行
2. 检查 `/api/system/version` 接口是否可访问
3. 查看浏览器控制台错误信息
4. 检查 CORS 配置

### 问题 2: 升级逻辑执行失败

**症状**: 升级过程中报错

**解决**:
1. 查看后端日志 `logs/viewx.log`
2. 检查数据库连接
3. 验证升级 SQL 语句
4. 确认权限配置

### 问题 3: 前端缓存问题

**症状**: 更新后仍显示旧版本

**解决**:
1. 清除浏览器缓存
2. 检查 Nginx 缓存配置
3. 确认 `index.html` 设置了 `no-cache`
4. 强制刷新 (Ctrl+Shift+R)

## 监控和日志

### 后端日志

```java
log.info("客户端版本 {} 需要更新到 {}", clientVersion, currentVersion);
log.info("开始执行版本升级: {} -> {}", fromVersion, toVersion);
log.info("版本升级完成: {} -> {}", fromVersion, toVersion);
log.error("版本升级失败: {} -> {}", fromVersion, toVersion, e);
```

### 前端日志

```typescript
console.log('版本检查器已初始化，当前版本:', version)
console.log('检测到新版本:', updateInfo.version)
console.error('检查更新失败:', error)
```

## 扩展功能

### 1. 灰度发布

可以在 `checkUpdate` 中添加用户分组逻辑：

```java
// 仅对特定用户组返回新版本
if (isInBetaGroup(userId)) {
    return getLatestVersion();
} else {
    return getStableVersion();
}
```

### 2. 版本回退

添加版本回退接口：

```java
@PostMapping("/rollback")
public Result<Void> rollback(@RequestParam String toVersion) {
    // 执行回退逻辑
}
```

### 3. 更新统计

记录用户更新情况：

```java
// 记录更新日志
updateLogService.record(userId, fromVersion, toVersion, success);
```

## 总结

ViewX 的版本管理系统提供了完整的版本检查、更新通知和升级迁移功能。通过合理使用这些功能，可以确保系统平滑升级，用户体验良好。

关键要点：
1. 前后端版本号保持同步
2. 升级逻辑要幂等且可回滚
3. 充分测试后再发布
4. 做好监控和日志记录
5. 准备应急回滚方案
