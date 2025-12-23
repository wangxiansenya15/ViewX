# Cloudflare Turnstile 人机验证使用指南

## ⚠️ 重要说明

**Cloudflare Turnstile 是独立的验证服务，不需要修改 DNS！**

- ✅ 不需要将域名 DNS 改为 Cloudflare
- ✅ 不需要使用 Cloudflare CDN
- ✅ 不会影响网站访问速度
- ✅ 完全免费，无限制使用
- ✅ 只需要注册获取 Site Key 和 Secret Key

---

## 📝 注册步骤

### 第一步：注册 Cloudflare 账号

1. 访问 https://dash.cloudflare.com/sign-up
2. 使用邮箱注册账号（免费）
3. 验证邮箱

### 第二步：创建 Turnstile Site

1. 登录后，访问 https://dash.cloudflare.com/?to=/:account/turnstile
2. 或者在左侧菜单找到 **"Turnstile"** 选项
3. 点击 **"Add Site"** 或 **"创建站点"**

### 第三步：填写站点信息

```
站点名称 (Site Name): ViewX 人机验证
域名 (Domains): 
  - viewx.com (您的实际域名)
  - localhost (用于本地开发测试)
  
Widget Mode (小部件模式):
  - 选择 "Managed" (推荐，自动选择最佳验证方式)
  - 或 "Non-Interactive" (无需用户交互)
  - 或 "Invisible" (完全隐藏)
```

### 第四步：获取密钥

创建成功后，您会看到：

```
Site Key: 0x4AAAAAAAxxxxxxxxxxxxxxxxxx
Secret Key: 0x4AAAAAAAyyyyyyyyyyyyyyyyyyyy
```

**重要：**
- **Site Key**: 用于前端，可以公开
- **Secret Key**: 用于后端验证，必须保密！

---

## 🔧 配置步骤

### 1. 配置后端 (application.yml)

```yaml
captcha:
  enabled: true
  type: turnstile
  site-key: 0x4AAAAAAAxxxxxxxxxxxxxxxxxx  # 替换为您的 Site Key
  secret-key: 0x4AAAAAAAyyyyyyyyyyyyyyyyyyyy  # 替换为您的 Secret Key
```

### 2. 前端使用

在您的 Vue 组件中：

```vue
<template>
  <CaptchaVerification
    type="turnstile"
    site-key="0x4AAAAAAAxxxxxxxxxxxxxxxxxx"
    @verified="onVerified"
  />
</template>

<script setup>
import CaptchaVerification from '@/components/CaptchaVerification.vue';

const onVerified = (token) => {
  console.log('验证成功，token:', token);
  // 将 token 发送到后端验证
};
</script>
```

---

## 🌐 工作原理

```
┌─────────────┐
│  用户浏览器  │
└──────┬──────┘
       │
       │ 1. 加载 Turnstile JS
       ▼
┌─────────────────────────┐
│ Cloudflare Turnstile    │
│ (challenges.cloudflare) │
└──────┬──────────────────┘
       │
       │ 2. 返回验证 Token
       ▼
┌─────────────┐
│  您的前端    │
└──────┬──────┘
       │
       │ 3. 提交表单 + Token
       ▼
┌─────────────┐
│  您的后端    │ ──── 4. 验证 Token ───▶ Cloudflare API
│ (阿里云服务器)│ ◀─── 5. 返回验证结果 ── (siteverify)
└─────────────┘
```

**关键点：**
- 您的网站仍然托管在阿里云
- DNS 解析仍然指向阿里云
- 只有验证组件会调用 Cloudflare 的 API
- 用户访问速度不受影响

---

## 🚀 完整示例

### 登录页面集成

```vue
<template>
  <form @submit.prevent="handleLogin">
    <input v-model="username" placeholder="用户名" />
    <input v-model="password" type="password" placeholder="密码" />
    
    <!-- Turnstile 验证 -->
    <CaptchaVerification
      ref="captchaRef"
      type="turnstile"
      site-key="0x4AAAAAAAxxxxxxxxxxxxxxxxxx"
      @verified="captchaToken = $event"
    />
    
    <button type="submit" :disabled="!captchaToken">登录</button>
  </form>
</template>

<script setup>
import { ref } from 'vue';
import axios from 'axios';
import CaptchaVerification from '@/components/CaptchaVerification.vue';

const username = ref('');
const password = ref('');
const captchaToken = ref('');
const captchaRef = ref(null);

const handleLogin = async () => {
  try {
    const response = await axios.post('/api/auth/login', {
      username: username.value,
      password: password.value,
      captchaToken: captchaToken.value
    });
    
    if (response.data.success) {
      console.log('登录成功');
    }
  } catch (error) {
    console.error('登录失败:', error);
    // 重置验证
    captchaRef.value?.reset();
    captchaToken.value = '';
  }
};
</script>
```

### 后端验证

```java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    // 1. 验证人机验证
    String remoteIp = getClientIp(httpRequest);
    boolean captchaValid = captchaService.verifyCaptcha(request.getCaptchaToken(), remoteIp);
    
    if (!captchaValid) {
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "message", "人机验证失败"
        ));
    }
    
    // 2. 验证用户名密码
    // ... 您的登录逻辑
    
    return ResponseEntity.ok(Map.of(
        "success", true,
        "token", jwtToken
    ));
}
```

---

## 🎨 Widget 模式说明

### Managed (推荐)
- Cloudflare 自动选择最佳验证方式
- 大多数情况下无需用户交互
- 可疑流量会显示挑战

### Non-Interactive
- 完全后台验证
- 用户无感知
- 适合不想打扰用户的场景

### Invisible
- 完全隐藏
- 需要手动触发验证
- 适合自定义 UI

---

## 📊 使用场景建议

### ✅ 必须使用
- 用户注册
- 用户登录（特别是失败多次后）
- 密码重置
- 发布内容（评论、视频、文章）
- 投票/点赞

### ⚠️ 建议使用
- 搜索功能（防止爬虫）
- 表单提交
- 下载资源
- API 频繁调用

### ❌ 不建议使用
- 普通页面浏览
- 查看个人信息
- 静态内容访问

---

## 🔒 安全最佳实践

1. **Secret Key 保密**
   - 永远不要在前端代码中暴露
   - 不要提交到 Git 仓库
   - 使用环境变量存储

2. **后端验证**
   - 前端验证只是 UI 层面
   - 必须在后端验证 Token
   - 每个 Token 只能使用一次

3. **IP 验证**
   - 验证时传递用户真实 IP
   - 防止 Token 被盗用

4. **超时处理**
   - Token 有效期约 5 分钟
   - 过期后需要重新验证

---

## 🌍 中国访问速度

### Turnstile 在中国的表现

✅ **可以正常访问**
- Cloudflare 的 API 在中国可以访问
- 加载速度可接受（通常 < 1 秒）
- 不需要翻墙

### 备选方案

如果担心 Cloudflare 访问速度，可以使用：

1. **滑块验证** (已实现)
   - 完全自主可控
   - 无需第三方服务
   - 速度最快

2. **腾讯云验证码**
   - 国内服务，速度快
   - 需要付费

3. **阿里云验证码**
   - 国内服务，速度快
   - 需要付费

---

## 🧪 测试

### 本地测试

1. 在 Turnstile 配置中添加 `localhost` 域名
2. 启动项目测试
3. 查看浏览器控制台确认加载成功

### 测试 Token

Cloudflare 提供测试用的 Site Key：

```
测试 Site Key: 1x00000000000000000000AA
测试 Secret Key: 1x0000000000000000000000000000000AA
```

这些密钥会始终返回成功，用于开发测试。

---

## ❓ 常见问题

### Q: 需要付费吗？
A: 完全免费，无限制使用。

### Q: 会影响网站速度吗？
A: 不会。只在需要验证时加载一个小脚本（约 50KB）。

### Q: 需要修改 DNS 吗？
A: **不需要！** 这是最常见的误解。

### Q: 在中国能用吗？
A: 可以，但建议测试一下加载速度。如果不满意，可以使用滑块验证。

### Q: 如何隐藏验证组件？
A: 选择 "Invisible" 模式，并手动触发验证。

### Q: Token 可以重复使用吗？
A: 不可以。每个 Token 只能验证一次。

---

## 📚 相关链接

- [Cloudflare Turnstile 官方文档](https://developers.cloudflare.com/turnstile/)
- [Turnstile 控制台](https://dash.cloudflare.com/?to=/:account/turnstile)
- [API 文档](https://developers.cloudflare.com/turnstile/get-started/server-side-validation/)

---

## 总结

✅ **您只需要：**
1. 注册 Cloudflare 账号（免费）
2. 创建 Turnstile Site
3. 获取 Site Key 和 Secret Key
4. 在代码中配置

❌ **您不需要：**
1. 修改 DNS 解析
2. 使用 Cloudflare CDN
3. 改变服务器配置
4. 担心访问速度变慢

**Turnstile 只是一个验证组件，就像使用 Google 字体一样简单！**
