# 主题系统文档 (Theming System)

本项目采用 CSS 变量 (CSS Custom Properties) 结合 Tailwind CSS 实现深色/亮色模式的无缝切换。

## 核心文件
- `src/styles/theme.scss`: 定义了所有的 CSS 变量。
- `src/styles/tailwind.css`: 引入 Tailwind 并定义了部分全局样式。
- `src/App.vue`: 在应用挂载时初始化 `data-theme` 属性。

## CSS 变量定义
我们在 `theme.scss` 中定义了以下核心变量，分别对应不同的主题模式：

```scss
[data-theme="light"] {
  --bg: #f8fafc;           // 背景色
  --bg-glass: rgba(255, 255, 255, 0.9); // 毛玻璃背景
  --text: #0f172a;         // 主要文本
  --text-secondary: #334155; // 次要文本
  --muted: #64748b;        // 弱化文本
  --primary: #2563eb;      // 主色调
  --border: #e2e8f0;       // 边框颜色
  // ...其他变量
}

[data-theme="dark"] {
  --bg: #0b0f14;
  --text: #e5e7eb;
  // ...对应深色值
}
```

## 使用规范
在编写组件样式时，**严禁使用硬编码的颜色值**（如 `#000000`, `bg-gray-900`）。
请使用 Tailwind 的任意值语法或自定义类来引用 CSS 变量：

**错误示例**:
```html
<div class="bg-white text-black">...</div>
```

**正确示例**:
```html
<div class="bg-[var(--bg)] text-[var(--text)]">...</div>
```
或者在 SCSS 中：
```scss
.card {
  background-color: var(--surface);
  border: 1px solid var(--border);
}
```

## 字体优化
为了在亮色模式下提供更好的阅读体验，我们针对不同模式调整了字体平滑设置：
- **深色模式**: 启用 `antialiased` (亚像素抗锯齿)。
- **亮色模式**: 设置为 `auto` (标准抗锯齿)，以增加字体粗细和对比度。
