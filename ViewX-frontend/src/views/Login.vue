<template>
  <div class="login-container" @mousemove="handleMouseMove">
    <!-- Back Button -->
    <button class="back-btn" @click="$emit('close')">
      <span class="icon">←</span>
      <span>{{ t('auth.backToHome') }}</span>
    </button>

    <!-- Liquid Background -->
    <div class="liquid-bg">
      <div class="blob blob-1" :style="blobStyle1"></div>
      <div class="blob blob-2" :style="blobStyle2"></div>
      <div class="blob blob-3" :style="blobStyle3"></div>
      <div class="blob blob-4" :style="blobStyle4"></div>
      <!-- Ripple Effects -->
      <div 
        v-for="ripple in ripples" 
        :key="ripple.id"
        class="ripple"
        :style="{ left: ripple.x + 'px', top: ripple.y + 'px' }"
      ></div>
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
            <button type="submit" class="submit-btn">
              <span>{{ t('auth.signIn') }}</span>
              <div class="liquid-btn-bg"></div>
            </button>
            <div class="switch-auth">
              {{ t('auth.noAccount') }} <span @click="isLogin = false">{{ t('auth.registerNow') }}</span>
            </div>
          </form>

          <!-- Register Form -->
          <!-- Register Form -->
          <form v-else key="register" class="form-section" @submit.prevent="handleRegister">
            <div class="input-group">
              <el-input 
                v-model="registerForm.username" 
                :placeholder="t('auth.username')" 
                :prefix-icon="User" 
                class="glass-input" 
              />
            </div>
            <div class="input-group">
              <el-input 
                v-model="registerForm.email" 
                :placeholder="t('auth.email')" 
                :prefix-icon="Message" 
                class="glass-input" 
              />
            </div>
            <div class="input-group flex gap-2">
              <el-input 
                v-model="registerForm.verificationCode" 
                placeholder="验证码" 
                :prefix-icon="Key" 
                class="glass-input flex-1" 
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
import { ref, reactive, computed } from 'vue'
import { User, Lock, Message, Key } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { Github } from 'lucide-vue-next'
import { authApi } from '@/api'
import { ElMessage } from 'element-plus'

const { t } = useI18n()

const isLogin = ref(true)
const loading = ref(false)
const codeLoading = ref(false)
const countDown = ref(60)
const isCountDown = ref(false)

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

// Mouse interaction logic
const mouseX = ref(0)
const mouseY = ref(0)
const ripples = ref<Array<{ id: number, x: number, y: number }>>([])
let rippleId = 0

const blobStyle1 = computed(() => ({ transform: `translate(${mouseX.value * 0.02}px, ${mouseY.value * 0.02}px)` }))
const blobStyle2 = computed(() => ({ transform: `translate(${mouseX.value * -0.02}px, ${mouseY.value * -0.02}px)` }))
const blobStyle3 = computed(() => ({ transform: `translate(${mouseX.value * 0.01}px, ${mouseY.value * -0.01}px)` }))
const blobStyle4 = computed(() => ({ transform: `translate(${mouseX.value * -0.01}px, ${mouseY.value * 0.01}px)` }))

function handleMouseMove(e: MouseEvent) {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
  
  // Create ripple occasionally
  if (Math.random() > 0.8) {
    createRipple(e.clientX, e.clientY)
  }
}

function createRipple(x: number, y: number) {
  const id = rippleId++
  ripples.value.push({ id, x, y })
  setTimeout(() => {
    ripples.value = ripples.value.filter(r => r.id !== id)
  }, 2000)
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
    console.log('Sending login data:', {
      username: loginForm.username,
      password: loginForm.password
    })
    const res = await authApi.login({
      username: loginForm.username,
      password: loginForm.password
    })
    
    localStorage.setItem('token', res.token)
    // Store user info if needed
    ElMessage.success('登录成功')
    emit('login-success')
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

<style lang="scss" scoped>
.login-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  background: #0f0c29;
  background: linear-gradient(to right, #24243e, #302b63, #0f0c29);
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
  filter: blur(80px);
  opacity: 0.6;

  .blob {
    position: absolute;
    border-radius: 50%;
    animation: float 10s infinite ease-in-out alternate;
  }

  .blob-1 {
    width: 400px;
    height: 400px;
    background: #ff00cc;
    top: -10%;
    left: -10%;
    animation-duration: 15s;
  }

  .blob-2 {
    width: 500px;
    height: 500px;
    background: #333399;
    bottom: -10%;
    right: -10%;
    animation-duration: 12s;
    animation-delay: -2s;
  }

  .blob-3 {
    width: 300px;
    height: 300px;
    background: #00d2ff;
    top: 40%;
    left: 60%;
    animation-duration: 18s;
    animation-delay: -5s;
  }

  .blob-4 {
    width: 350px;
    height: 350px;
    background: #ff9966;
    bottom: 20%;
    left: 10%;
    animation-duration: 20s;
    animation-delay: -8s;
  }
  
  .ripple {
    position: absolute;
    width: 2px;
    height: 2px;
    border-radius: 50%;
    border: 1px solid rgba(255, 255, 255, 0.3);
    animation: rippleEffect 2s linear forwards;
    pointer-events: none;
  }
}

@keyframes rippleEffect {
  0% {
    transform: scale(1);
    opacity: 0.5;
    border-width: 1px;
  }
  100% {
    transform: scale(100);
    opacity: 0;
    border-width: 0;
  }
}

.back-btn {
  position: absolute;
  top: 30px;
  left: 30px;
  z-index: 20;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  padding: 10px 20px;
  border-radius: 30px;
  color: white;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  .icon {
    font-size: 1.2rem;
    transition: transform 0.3s ease;
  }
  
  &:hover {
    background: rgba(255, 255, 255, 0.2);
    transform: translateX(5px);
    
    .icon {
      transform: translateX(-3px);
    }
  }
}

@keyframes float {
  0% { transform: translate(0, 0) rotate(0deg); }
  100% { transform: translate(50px, 50px) rotate(20deg); }
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
      
      // Fix browser autofill style
      &:-webkit-autofill,
      &:-webkit-autofill:hover,
      &:-webkit-autofill:focus,
      &:-webkit-autofill:active {
        -webkit-text-fill-color: white !important;
        // Use a color that matches the input background (rgba(0, 0, 0, 0.2) mixed with the dark background)
        // Since we can't use rgba with alpha for autofill background in some browsers properly without the hack,
        // we use a solid color that approximates the look, or use the transition hack with the exact rgba.
        // Here we try to match the input's focus state background.
        -webkit-box-shadow: 0 0 0 1000px #2a2a35 inset !important; 
        transition: background-color 5000s ease-in-out 0s;
        caret-color: white;
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
      width: 40px;
      height: 40px;
      border-radius: 10px;
      background: rgba(255, 255, 255, 0.1);
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.3s;
      border: 1px solid rgba(255, 255, 255, 0.05);

      &:hover {
        background: rgba(255, 255, 255, 0.2);
        transform: translateY(-2px);
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
  padding: 0 15px;
  transition: all 0.3s;
  
  &:hover:not(:disabled) {
    background: rgba(255, 255, 255, 0.2);
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
