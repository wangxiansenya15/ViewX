# 更新日志 (Changelog)

## [Unreleased] - 2025-11-27

### 新增功能 (Features)
- **启动屏 (Splash Screen)**: 添加了类似 YouTube 移动端的启动加载动画。
  - 包含 Logo 悬浮动画和弹性进度条。
  - 支持中英文 Slogan 切换。
- **国际化 (i18n)**: 引入 `vue-i18n` 支持多语言切换。
  - 支持简体中文 (zh-CN) 和 英文 (en-US)。
  - 覆盖了设置页面、侧边栏导航和启动屏。

### 修复与优化 (Fixes & Improvements)
- **主题适配 (Theming)**:
  - 修复了亮色模式 (Light Mode) 下的显示问题。
  - 移除了硬编码的颜色值，全面采用 CSS 变量 (`var(--bg)`, `var(--text)` 等)。
  - 优化了亮色模式下的字体清晰度（禁用了亚像素抗锯齿）。
  - 修复了 `NavBar`、`Sidebar`、`VideoCard` 等组件在亮色模式下的颜色显示异常。
- **代码规范**:
  - 添加了 `.cursorrules` 文件以规范 AI 辅助开发。
  - 修复了 `App.vue` 和 `Settings.vue` 中的未使用变量和 CSS 兼容性警告。
