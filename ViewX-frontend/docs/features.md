# 功能特性文档 (Features)

## 启动屏 (Splash Screen)
位于 `src/components/SplashScreen.vue`。

### 效果描述
- 类似于 YouTube 移动端 App 的启动效果。
- 包含一个带有弹性动画 (Spring Animation) 的 Logo。
- 底部有一个带有渐变色的进度条。
- 仅在应用首次加载 (`isInitialLoad`) 时显示。

### 实现细节
- 使用 Vue 的 `<Transition>` 组件实现淡出效果。
- 进度条动画使用 CSS `transition` 配合 `cubic-bezier` 实现弹性舒缓效果。
- 状态由 `App.vue` 中的 `isLoading` 和 `isInitialLoad` 变量控制。

## 视频流布局 (Video Masonry)
位于 `src/components/VideoMasonry.vue`。

- 实现了响应式网格布局。
- 顶部包含横向滚动的分类标签栏。
- 适配了亮色/深色模式的样式。

## 侧边栏 (Sidebar)
位于 `src/components/Sidebar.vue`。

- 响应式设计：在移动端隐藏，桌面端显示。
- 包含导航菜单和 Gemini AI 功能推广卡片。
- 集成了国际化支持。
