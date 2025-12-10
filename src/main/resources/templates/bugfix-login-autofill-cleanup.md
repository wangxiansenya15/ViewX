# Bug 修复总结：登录页 Chrome 自动填充样式与后端代码清理

## 1. 前端 Bug 修复

### 🐛 问题描述
**现象**：用户反馈 Chrome 浏览器在自动填充（Autofill）模式下，登录输入框显示不清晰。
**表现**：输入框背景变成了深色不透明块（`#2a2a35`），破坏了原本的玻璃拟态（Glassmorphism）半透明设计，导致视觉上突兀且文字对比度可能降低。

### 🔍 原因分析
原代码尝试通过 `box-shadow` inset 属性强制覆盖 Chrome 默认的淡黄色自动填充背景：

```scss
// 原代码
-webkit-box-shadow: 0 0 0 1000px #2a2a35 inset !important; 
transition: background-color 5000s ease-in-out 0s;
```

虽然这解决了一部分问题（去掉了默认的淡黄色），但引入了新的问题：它强行填充了一个实色背景 `#2a2a35`。由于我们的设计是动态背景上的毛玻璃效果，任何实色背景都会显得不自然且不透明，从而导致“显示不清晰”的视觉体验。

### ✅ 解决方案
移除了强制的 `box-shadow` 实色填充，完全利用 CSS `transition` 的特性，将 `background-color` 的变化延迟到一个极长的时间（99999s）。这样，浏览器试图应用默认填充色时，实际上会被无限推迟，从而保留了输入框原本的透明（或半透明）背景样式。

**修正后的解决方案**：
不仅移除强制的 `box-shadow` 实色填充，还增强了 `transition` hack，使其同时作用于 `background-color` 和 `color`。这样可以阻止浏览器在自动填充时修改背景色和文字颜色。

**修改后的代码**：
```scss
// File: ViewX-frontend/src/views/Login.vue

&:-webkit-autofill,
&:-webkit-autofill:hover,
&:-webkit-autofill:focus,
&:-webkit-autofill:active {
  -webkit-text-fill-color: white !important;
  // Use transition hack to keep the background transparent AND text color white
  transition: background-color 99999s ease-in-out 0s, color 99999s ease-in-out 0s; // 关键修改
  caret-color: white;
}
```

**效果**：
1. **背景透明**：`transition: background-color ...` 阻止背景变黄/变黑，保留玻璃质感。
2. **文字清晰**：`transition: color ...` 阻止文字变黑，保持预设的白色，解决深色背景下文字看不清的问题。

---

## 2. 后端代码清理

### 🛠️ 优化变动
在处理前端问题的间隙，IDE 提示 `ActionLogConsumer.java` 中存在未使用的导入。

**文件**：`src/main/java/com/flowbrain/viewx/service/consumer/ActionLogConsumer.java`
**变动**：移除了 `import java.time.LocalDateTime;`。
**原因**：该类中的时间字段现已通过 MyBatis-Plus 的自动填充策略（`@TableField(fill = FieldFill.INSERT)`）在实体类 `ActionLog` 中处理，不再需要在 Consumer 中手动 `setCreatedAt(LocalDateTime.now())`，因此该导入变得多余。

---

**修复时间**：2025-12-10
**相关文件**：
- `ViewX-frontend/src/views/Login.vue`
- `src/main/java/com/flowbrain/viewx/service/consumer/ActionLogConsumer.java`
