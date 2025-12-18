<template>
  <div class="login-container" @mousemove="handleMouseMove">
    <!-- Back Button -->
    <button class="back-btn" @click="goBack">
      <ArrowLeft class="icon" :size="20" />
      <span>{{ t('auth.backToHome') }}</span>
    </button>

    <!-- Liquid Background JetBrains Style -->
    <div class="liquid-bg">
      <div class="noise-bg"></div>
      <canvas ref="particleCanvas" class="particles-canvas"></canvas>
      <div class="aurora-blobs">
        <div class="blob-wrapper" :style="blobStyle1">
          <div class="blob-item blob-1"></div>
        </div>
        <div class="blob-wrapper" :style="blobStyle2">
          <div class="blob-item blob-2"></div>
        </div>
        <div class="blob-wrapper" :style="blobStyle3">
          <div class="blob-item blob-3"></div>
        </div>
        <div class="blob-wrapper" :style="blobStyle4">
          <div class="blob-item blob-4"></div>
        </div>
      </div>
    </div>

    <!-- Glassmorphism Card -->
    <div class="glass-card">
      <div class="card-content">
        <div class="header">
          <h1 class="title">ViewX</h1>
          <p class="subtitle">{{ t('auth.subtitle') }}</p>
        </div>

        <!-- Toggle Switch -->
        <div class="auth-toggle">
          <span :class="{ active: isLogin }" @click="isLogin = true">{{ t('auth.login') }}</span>
          <span :class="{ active: !isLogin }" @click="isLogin = false">{{ t('auth.register') }}</span>
          <div class="slider" :class="{ right: !isLogin }"></div>
        </div>

        <!-- Login Form -->
        <transition name="fade-slide" mode="out-in">
          <form v-if="isLogin" key="login" class="form-section" @submit.prevent="handleLogin">
            <div class="input-group">
              <el-input 
                v-model="loginForm.username" 
                :placeholder="t('auth.username')" 
                :prefix-icon="User" 
                class="glass-input" 
              />
            </div>
            <div class="input-group">
              <el-input
                v-model="loginForm.password"
                type="password"
                :placeholder="t('auth.password')"
                :prefix-icon="Lock"
                show-password
                class="glass-input"
              />
            </div>
            <div class="actions">
              <el-checkbox v-model="loginForm.remember" class="glass-checkbox">{{ t('auth.rememberMe') }}</el-checkbox>
              <a href="#" class="forgot-link">{{ t('auth.forgotPassword') }}</a>
            </div>
            <button type="submit" class="submit-btn" :disabled="loading">
              <span>{{ loading ? 'Signing in...' : t('auth.signIn') }}</span>
              <div class="liquid-btn-bg"></div>
            </button>
            <div class="switch-auth">
              {{ t('auth.noAccount') }} <span @click="isLogin = false">{{ t('auth.registerNow') }}</span>
            </div>
          </form>

          <!-- Register Form -->
          <form v-else key="register" class="form-section" @submit.prevent="handleRegister" autocomplete="off">
            <!-- 欺骗浏览器的隐藏输入框，防止自动填充第一项 -->
            <input type="text" style="display:none" />
            <input type="password" style="display:none" />

            <div class="input-group">
              <el-input 
                v-model="registerForm.username" 
                :placeholder="t('auth.username')" 
                :prefix-icon="User" 
                class="glass-input" 
                name="register-username"
                autocomplete="off"
              />
            </div>
            <div class="input-group">
              <el-input 
                v-model="registerForm.email" 
                :placeholder="t('auth.email')" 
                :prefix-icon="Message" 
                class="glass-input" 
                name="register-email"
                autocomplete="off"
              />
            </div>
            <div class="input-group flex gap-2">
              <el-input 
                v-model="registerForm.verificationCode" 
                placeholder="验证码" 
                :prefix-icon="Key" 
                class="glass-input flex-1" 
                name="verification-code"
                autocomplete="off"
              />
              <el-button type="button" :disabled="isCountDown" @click="handleGetCode" class="code-btn" :loading="codeLoading">
                {{ isCountDown ? `${countDown}s` : '获取验证码' }}
              </el-button>
            </div>
            <div class="input-group">
              <el-input
                v-model="registerForm.password"
                type="password"
                :placeholder="t('auth.password')"
                :prefix-icon="Lock"
                show-password
                class="glass-input"
                name="new-password"
                autocomplete="new-password"
              />
            </div>
             <button type="submit" class="submit-btn" :disabled="loading">
              <span>{{ loading ? 'Processing...' : t('auth.createAccount') }}</span>
              <div class="liquid-btn-bg"></div>
            </button>
            <div class="switch-auth">
              {{ t('auth.hasAccount') }} <span @click="isLogin = true">{{ t('auth.loginNow') }}</span>
            </div>
          </form>
        </transition>

        <div class="social-login">
          <p>{{ t('auth.orContinueWith') }}</p>
          <div class="social-icons">
            <div class="icon-btn github" @click="handleGithubLogin">
              <Github class="w-5 h-5" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { User, Lock, Message, Key } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { Github, ArrowLeft } from 'lucide-vue-next'
import { authApi } from '@/api'
import { ElMessage } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const isLogin = ref(true)
const loading = ref(false)
const codeLoading = ref(false)
const countDown = ref(60)
const isCountDown = ref(false)

// Particle System
const particleCanvas = ref<HTMLCanvasElement | null>(null)
let ctx: CanvasRenderingContext2D | null = null
let animationFrameId: number
const particles: Particle[] = []

// 性能优化:减少粒子数量和连接距离
const PARTICLE_COUNT = 30 // 从80降低到30
const CONNECTION_DISTANCE = 120 // 从150降低到120
const MOUSE_DISTANCE = 150 // 从200降低到150

class Particle {
  x: number
  y: number
  vx: number
  vy: number
  size: number
  color: string
  
  constructor(canvasWidth: number, canvasHeight: number) {
    this.x = Math.random() * canvasWidth
    this.y = Math.random() * canvasHeight
    this.vx = (Math.random() - 0.5) * 0.8 // Random velocity
    this.vy = (Math.random() - 0.5) * 0.8
    this.size = Math.random() * 2 + 1
    this.color = `rgba(255, 255, 255, ${Math.random() * 0.5 + 0.2})`
  }

  update(width: number, height: number, mouseX: number, mouseY: number) {
    this.x += this.vx
    this.y += this.vy

    // Mouse Attraction (Aggregation)
    const dx = mouseX - this.x
    const dy = mouseY - this.y
    const distance = Math.sqrt(dx * dx + dy * dy)
    
    if (distance < MOUSE_DISTANCE) {
      const forceDirectionX = dx / distance
      const forceDirectionY = dy / distance
      const force = (MOUSE_DISTANCE - distance) / MOUSE_DISTANCE
      // Gentle attraction force
      const attractionStrength = 0.05
      this.vx += forceDirectionX * force * attractionStrength
      this.vy += forceDirectionY * force * attractionStrength
    }

    // Friction to prevent infinite acceleration
    this.vx *= 0.99
    this.vy *= 0.99

    // Boundary check (Bounce)
    if (this.x < 0 || this.x > width) this.vx = -this.vx
    if (this.y < 0 || this.y > height) this.vy = -this.vy
  }

  draw(context: CanvasRenderingContext2D) {
    context.beginPath()
    context.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    context.fillStyle = this.color
    context.fill()
  }
}

onMounted(() => {
  if (particleCanvas.value) {
    const canvas = particleCanvas.value
    ctx = canvas.getContext('2d')
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight

    // Initialize particles
    for (let i = 0; i < PARTICLE_COUNT; i++) {
      particles.push(new Particle(canvas.width, canvas.height))
    }

    const animate = () => {
      if (!ctx || !canvas) return
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      
      // Update and Draw Particles
      particles.forEach(p => {
        p.update(canvas.width, canvas.height, mouseX.value, mouseY.value)
        p.draw(ctx!)
      })

      // Draw Connections (Aggregation)
      connectParticles(particles, ctx)
      
      animationFrameId = requestAnimationFrame(animate)
    }
    animate()

    window.addEventListener('resize', () => {
      canvas.width = window.innerWidth
      canvas.height = window.innerHeight
    })
  }
})

function connectParticles(particles: Particle[], ctx: CanvasRenderingContext2D) {
  for (let i = 0; i < particles.length; i++) {
    for (let j = i; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const distance = Math.sqrt(dx * dx + dy * dy)

      if (distance < CONNECTION_DISTANCE) {
        ctx.beginPath()
        const opacity = 1 - (distance / CONNECTION_DISTANCE)
        ctx.strokeStyle = `rgba(255, 255, 255, ${opacity * 0.2})` // Subtle lines
        ctx.lineWidth = 1
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.stroke()
      }
    }
  }
}

onUnmounted(() => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
})


const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  verificationCode: ''
})

const emit = defineEmits(['login-success', 'close'])

// Mouse interaction logic (Reused for Parallax)
const mouseX = ref(0)
const mouseY = ref(0)

const blobStyle1 = computed(() => ({ transform: `translate(${mouseX.value * 0.04}px, ${mouseY.value * 0.04}px)` }))
const blobStyle2 = computed(() => ({ transform: `translate(${mouseX.value * -0.04}px, ${mouseY.value * -0.04}px)` }))
const blobStyle3 = computed(() => ({ transform: `translate(${mouseX.value * 0.02}px, ${mouseY.value * -0.02}px)` }))
const blobStyle4 = computed(() => ({ transform: `translate(${mouseX.value * -0.02}px, ${mouseY.value * 0.02}px)` }))

function handleMouseMove(e: MouseEvent) {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

const goBack = () => {
    if (route.path === '/login') {
        router.push('/')
    } else {
        emit('close')
    }
}

const handleGetCode = async () => {
  if (!registerForm.email) {
    ElMessage.warning('请输入邮箱')
    return
  }
  
  try {
    codeLoading.value = true
    await authApi.getVerificationCode(registerForm.email)
    ElMessage.success('验证码已发送')
    
    // Start countdown
    isCountDown.value = true
    const timer = setInterval(() => {
      countDown.value--
      if (countDown.value <= 0) {
        clearInterval(timer)
        isCountDown.value = false
        countDown.value = 60
      }
    }, 1000)
  } catch (error) {
    // Error handled by interceptor
  } finally {
    codeLoading.value = false
  }
}

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    loading.value = true
    const res = await authApi.login({
      username: loginForm.username,
      password: loginForm.password
    })
    
    // 保存用户信息到 store
    const userStore = useUserStore()
    userStore.setToken(res.token)
    
    // 从登录响应中提取用户信息并保存
    if (res.userInfo) {
      userStore.setUserInfo({
        id: res.userInfo.id,
        username: res.userInfo.username,
        nickname: res.userInfo.username, // 使用 username 作为 nickname
        avatar: res.userInfo.avatar || ''
      })
    }
    
    ElMessage.success('登录成功')
    
    if (route.path === '/login') {
        // Reload page to refresh auth state in App.vue or just push and let App.vue re-check
        // Since App.vue checks on mounted, we might need a global state refresh or just reload
        // Simplest for now: redirect home and reload if needed, OR trigger an event bus
        window.location.href = '/'
    } else {
        emit('login-success')
    }
  } catch (error) {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!registerForm.email || !registerForm.password || !registerForm.username || !registerForm.verificationCode) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    loading.value = true
    await authApi.register({
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password,
      verificationCode: registerForm.verificationCode
    })
    
    ElMessage.success('注册成功，请登录')
    isLogin.value = true // Switch to login tab
  } catch (error) {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleGithubLogin = () => {
  // Redirect to backend OAuth2 endpoint which handles the GitHub handshake
  const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080'
  window.location.href = `${backendUrl}/api/oauth2/authorization/github`
}
</script>

<style lang="scss">
/* 全局样式覆盖 - 解决 Chrome Autofill 样式顽固问题 */
/* 使用更高优先级的选择器，针对 Element Plus 的内部 input 类名 */
.login-container {
  .el-input__inner:-webkit-autofill,
  .el-input__inner:-webkit-autofill:hover,
  .el-input__inner:-webkit-autofill:focus,
  .el-input__inner:-webkit-autofill:active {
    -webkit-text-fill-color: #ffffff !important;
    transition: background-color 99999s ease-in-out 0s !important;
    background-color: transparent !important;
    background-image: none !important;
    color: #ffffff !important;
  }
}

@keyframes float-shape {
  0% {
    transform: translate(0, 0) rotate(0deg) scale(1);
    border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%;
  }
  33% {
    transform: translate(30px, -50px) rotate(10deg) scale(1.1);
    border-radius: 40% 60% 70% 30% / 40% 50% 60% 50%;
  }
  66% {
    transform: translate(-20px, 20px) rotate(-5deg) scale(0.9);
    border-radius: 70% 30% 50% 50% / 30% 40% 50% 60%;
  }
  100% {
    transform: translate(0, 0) rotate(0deg) scale(1);
    border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%;
  }
}
</style>

<style lang="scss" scoped>
.login-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  background: #000000;
  font-family: 'Inter', sans-serif;
}

/* Liquid Background Animation */
.liquid-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  overflow: hidden;
  background: #000;
}

.noise-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 2;
  opacity: 0.07; /* 稍微增加噪点可见度 */
  pointer-events: none;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)' opacity='1'/%3E%3C/svg%3E");
}

.particles-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1; /* 与 Blobs 同层或稍高，但在 Noise 之下 */
  pointer-events: none;
}

.aurora-blobs {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  /* 移除容器上的背景色和模糊，改在 Blob 上 */
}

.blob-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

.blob-item {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px); // 从60px降低到40px,提升性能
  mix-blend-mode: screen;
  opacity: 1; // 确保可见
  animation: float-shape 10s infinite ease-in-out alternate;
  will-change: transform, border-radius;
}


/* 重新定义 Blob 的颜色和形态 - 模拟极光 */
.blob-1 {
  top: -10%;
  left: -10%;
  width: 50vw;
  height: 50vw;
  background: linear-gradient(135deg, #FF2975 0%, #7B1FA2 100%);
  opacity: 0.8;
  animation-duration: 12s;
}

.blob-2 {
  bottom: -20%;
  right: -10%;
  width: 60vw;
  height: 60vw;
  background: linear-gradient(135deg, #4361EE 0%, #3F37C9 100%);
  opacity: 0.8;
  animation-duration: 15s;
  animation-delay: -2s;
}

.blob-3 {
  top: 30%;
  left: 40%;
  width: 40vw;
  height: 40vw;
  background: linear-gradient(135deg, #0cebeb 0%, #20e3b2 100%);
  opacity: 0.6;
  animation-duration: 11s;
  animation-delay: -4s;
}

.blob-4 {
  bottom: 10%;
  left: 10%;
  width: 45vw;
  height: 45vw;
  background: linear-gradient(135deg, #7209b7 0%, #f72585 100%);
  opacity: 0.7;
  animation-duration: 14s;
  animation-delay: -6s;
}



.back-btn {
  position: absolute;
  top: 40px;
  left: 40px;
  z-index: 20;
  
  // Glassmorphism Base
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  
  padding: 12px 24px;
  border-radius: 9999px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  letter-spacing: 0.5px;
  
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  
  .icon {
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }
  
  &:hover {
    background: rgba(255, 255, 255, 0.15);
    border-color: rgba(255, 255, 255, 0.3);
    transform: translateY(-2px);
    box-shadow: 
      0 10px 20px -5px rgba(0, 0, 0, 0.2), 
      0 0 15px rgba(255, 255, 255, 0.1); // Glow
    
    .icon {
      transform: translateX(-4px);
    }
  }
  
  &:active {
    transform: translateY(0) scale(0.96);
    background: rgba(255, 255, 255, 0.1);
  }
}


/* Glassmorphism Card */
.glass-card {
  position: relative;
  z-index: 10;
  width: 420px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  color: white;
  transition: transform 0.3s ease;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.6);
  }
}

.header {
  text-align: center;
  margin-bottom: 30px;

  .title {
    font-size: 2.5rem;
    font-weight: 700;
    margin: 0;
    background: linear-gradient(135deg, #fff 0%, #a5b4fc 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    letter-spacing: -1px;
  }

  .subtitle {
    margin: 8px 0 0;
    color: rgba(255, 255, 255, 0.6);
    font-size: 0.95rem;
  }
}

/* Toggle Switch */
.auth-toggle {
  display: flex;
  position: relative;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 30px;
  cursor: pointer;

  span {
    flex: 1;
    text-align: center;
    padding: 10px;
    font-size: 0.9rem;
    font-weight: 500;
    color: rgba(255, 255, 255, 0.6);
    z-index: 2;
    transition: color 0.3s;

    &.active {
      color: white;
    }
  }

  .slider {
    position: absolute;
    top: 4px;
    left: 4px;
    width: calc(50% - 4px);
    height: calc(100% - 8px);
    background: rgba(255, 255, 255, 0.1);
    border-radius: 8px;
    transition: transform 0.3s cubic-bezier(0.4, 0.0, 0.2, 1);
    z-index: 1;

    &.right {
      transform: translateX(100%);
    }
  }
}

/* Form Styles */
.input-group {
  margin-bottom: 20px;

  :deep(.el-input__wrapper) {
    background: rgba(0, 0, 0, 0.2);
    box-shadow: none;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 12px;
    padding: 8px 15px;
    transition: all 0.3s;

    &:hover, &.is-focus {
      background: rgba(0, 0, 0, 0.3);
      border-color: rgba(255, 255, 255, 0.3);
      box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1);
    }

    .el-input__inner {
      color: white;
      height: 32px;
      
      &::placeholder {
        color: rgba(255, 255, 255, 0.4);
      }
    }

    .el-icon {
      color: rgba(255, 255, 255, 0.6);
    }
  }
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  font-size: 0.85rem;

  :deep(.el-checkbox) {
    color: rgba(255, 255, 255, 0.7);
    
    .el-checkbox__inner {
      background: rgba(255, 255, 255, 0.1);
      border-color: rgba(255, 255, 255, 0.3);
    }
    
    &.is-checked .el-checkbox__inner {
      background: #6366f1;
      border-color: #6366f1;
    }
  }

  .forgot-link {
    color: rgba(255, 255, 255, 0.6);
    text-decoration: none;
    transition: color 0.2s;

    &:hover {
      color: white;
    }
  }
}

/* Liquid Button */
.submit-btn {
  position: relative;
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: white;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.2s;
  
  span {
    position: relative;
    z-index: 2;
  }

  .liquid-btn-bg {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, #4f46e5, #06b6d4, #4f46e5);
    background-size: 200% 100%;
    animation: gradientMove 3s linear infinite;
    z-index: 1;
    transition: all 0.3s;
  }

  &:hover {
    transform: scale(1.02);
    
    .liquid-btn-bg {
      filter: brightness(1.1);
    }
  }

  &:active {
    transform: scale(0.98);
  }
}

@keyframes gradientMove {
  0% { background-position: 0% 50%; }
  100% { background-position: 100% 50%; }
}

/* Social Login */
.social-login {
  margin-top: 30px;
  text-align: center;

  p {
    color: rgba(255, 255, 255, 0.4);
    font-size: 0.85rem;
    margin-bottom: 15px;
    position: relative;
    
    &::before, &::after {
      content: '';
      position: absolute;
      top: 50%;
      width: 30%;
      height: 1px;
      background: rgba(255, 255, 255, 0.1);
    }
    
    &::before { left: 0; }
    &::after { right: 0; }
  }

  .social-icons {
    display: flex;
    justify-content: center;
    gap: 15px;

    .icon-btn {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      background: rgba(255, 255, 255, 0.08);
      backdrop-filter: blur(10px);
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      border: 1px solid rgba(255, 255, 255, 0.1);
      position: relative;
      overflow: hidden;

      // GitHub 图标特殊样式
      &.github {
        :deep(svg) {
          width: 24px;
          height: 24px;
          color: white;
          filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3));
        }
      }

      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), transparent);
        opacity: 0;
        transition: opacity 0.3s;
      }

      &:hover {
        background: rgba(255, 255, 255, 0.15);
        border-color: rgba(255, 255, 255, 0.2);
        transform: translateY(-3px);
        box-shadow: 0 8px 16px rgba(0, 0, 0, 0.3);

        &::before {
          opacity: 1;
        }
      }

      &:active {
        transform: translateY(-1px);
      }
    }
  }
}

/* Transitions */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.code-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  border-radius: 12px;
  padding: 0 20px;
  height: 48px; // 匹配输入框高度
  min-width: 110px;
  font-size: 0.9rem;
  transition: all 0.3s;
  
  &:hover:not(:disabled) {
    background: rgba(255, 255, 255, 0.2);
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
  }
  
  &:disabled {
    background: rgba(255, 255, 255, 0.05);
    color: rgba(255, 255, 255, 0.3);
    border-color: rgba(255, 255, 255, 0.05);
  }
}

.switch-auth {
  margin-top: 20px;
  text-align: center;
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.6);
  
  span {
    color: #a5b4fc;
    cursor: pointer;
    font-weight: 600;
    margin-left: 5px;
    transition: color 0.3s;
    
    &:hover {
      color: white;
      text-decoration: underline;
    }
  }
}
</style>
