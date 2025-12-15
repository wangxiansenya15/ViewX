<template>
  <div class="settings-container">
    <div class="settings-header relative flex items-center justify-center mb-8 py-2">
      <button @click="$router.push('/')" class="absolute left-0 z-10 p-2 text-[var(--text)]/80 hover:text-[var(--text)] rounded-full bg-[var(--surface)] border border-[var(--border)] backdrop-blur-md transition-colors">
        <ArrowLeft class="w-6 h-6" />
      </button>
      <h1 class="page-title text-2xl font-bold text-[var(--text)] text-center leading-relaxed tracking-wide z-0">{{ t('settings.title') }}</h1>
    </div>

    <div class="settings-content pb-20">
      <!-- 基础设置 -->
      <section class="settings-section glass-panel">
        <h2 class="section-title">
          <span class="icon">🎨</span>
          {{ t('settings.appearance.title') }}
        </h2>
        
        <div class="setting-item">
          <div class="setting-info">
            <transition name="slide-fade" mode="out-in">
              <span :key="isDarkMode" class="label block">
                 {{ isDarkMode ? t('settings.appearance.darkMode') : t('settings.appearance.lightMode') }}
              </span>
            </transition>
            <span class="description">{{ t('settings.appearance.darkModeDesc') }}</span>
          </div>
          <el-switch
            v-model="isDarkMode"
            active-color="#6366f1"
          />
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <span class="label">{{ t('settings.appearance.language') }}</span>
            <span class="description">{{ t('settings.appearance.languageDesc') }}</span>
          </div>
          <el-select v-model="locale" class="language-select">
            <el-option label="简体中文" value="zh-CN" />
            <el-option label="English" value="en-US" />
          </el-select>
        </div>
      </section>

      <!-- 账号安全 (需要登录) -->
      <section class="settings-section glass-panel" :class="{ 'locked': !isLoggedIn }">
        <h2 class="section-title">
          <span class="icon">🛡️</span>
          {{ t('settings.security.title') }}
          <span v-if="!isLoggedIn" class="badge-lock">{{ t('settings.security.requireLogin') }}</span>
        </h2>

        <div v-if="!isLoggedIn" class="login-placeholder">
          <p>{{ t('settings.security.loginPlaceholder') }}</p>
          <el-button type="primary" round @click="$router.push('/login')">{{ t('settings.security.loginBtn') }}</el-button>
        </div>

        <div v-else class="secure-settings">
          <div class="setting-item">
            <div class="setting-info">
              <span class="label">{{ t('settings.security.changePassword') }}</span>
              <span class="description">{{ t('settings.security.changePasswordDesc') }}</span>
            </div>
            <el-button plain size="small">{{ t('settings.security.change') }}</el-button>
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <span class="label">{{ t('settings.security.2fa') }}</span>
              <span class="description">{{ t('settings.security.2faDesc') }}</span>
            </div>
            <el-switch v-model="twoFactorEnabled" active-color="#6366f1" />
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <span class="label">{{ t('settings.security.loginHistory') }}</span>
              <span class="description">{{ t('settings.security.loginHistoryDesc') }}</span>
            </div>
            <el-button link>{{ t('settings.security.viewRecord') }}</el-button>
          </div>
          
          <div class="setting-item danger-zone">
            <div class="setting-info">
              <span class="label text-danger">{{ t('settings.security.deleteAccount') }}</span>
              <span class="description">{{ t('settings.security.deleteAccountDesc') }}</span>
            </div>
            <el-button type="danger" plain size="small">{{ t('settings.security.delete') }}</el-button>
          </div>
        </div>
      </section>

      <!-- 账号操作 (退出/切换) -->
      <section v-if="isLoggedIn" class="settings-section glass-panel">
         <h2 class="section-title">
            <span class="icon">👤</span>
            账号操作
         </h2>
         
         <div class="flex flex-col gap-3">
             <button @click="handleSwitchAccount" class="flex items-center justify-between w-full p-3 rounded-xl bg-white/5 hover:bg-white/10 transition-colors text-left group">
                 <div class="flex items-center gap-3">
                     <div class="p-2 rounded-full bg-indigo-500/20 text-indigo-400 group-hover:text-indigo-300">
                         <Users class="w-5 h-5" />
                     </div>
                     <div class="flex flex-col">
                         <span class="font-medium text-white">切换账号</span>
                         <span class="text-xs text-gray-500">登录另一个账号</span>
                     </div>
                 </div>
                 <ChevronRight class="w-5 h-5 text-gray-500" />
             </button>

             <button @click="handleLogout" class="flex items-center justify-between w-full p-3 rounded-xl bg-red-500/10 hover:bg-red-500/20 transition-colors text-left group">
                 <div class="flex items-center gap-3">
                     <div class="p-2 rounded-full bg-red-500/20 text-red-500 group-hover:text-red-400">
                         <LogOut class="w-5 h-5" />
                     </div>
                     <span class="font-medium text-red-500 group-hover:text-red-400">退出登录</span>
                 </div>
             </button>
         </div>
      </section>

      <!-- 关于信息 -->
      <section class="settings-section glass-panel">
        <h2 class="section-title">
          <span class="icon">ℹ️</span>
          {{ t('settings.about.title') }}
        </h2>
        
        <div class="setting-item">
          <div class="setting-info">
            <span class="label">{{ t('settings.about.version') }}</span>
            <span class="description">{{ appVersion }}</span>
          </div>
          <el-button 
            link 
            type="primary" 
            @click="handleCheckUpdate"
            :loading="checkingUpdate"
          >
            {{ checkingUpdate ? '检查中...' : t('settings.about.checkUpdate') }}
          </el-button>
        </div>
        
        <div class="setting-item">
          <div class="setting-info">
            <span class="label">{{ t('settings.about.browserInfo') }}</span>
            <span class="description">{{ browserInfo }}</span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowLeft, LogOut, Users, ChevronRight } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { getVersionChecker } from '@/utils/versionChecker'

const props = defineProps<{
  isLoggedIn: boolean
  theme: 'light' | 'dark'
}>()

const emit = defineEmits<{
  'toggle-theme': []
  'require-login': []
}>()

const { t, locale } = useI18n()
const router = useRouter()

const isDarkMode = computed({
  get: () => props.theme === 'dark',
  set: () => emit('toggle-theme')
})

const twoFactorEnabled = ref(false)
const checkingUpdate = ref(false)

// 应用版本（从环境变量或package.json读取）
const appVersion = computed(() => {
  return import.meta.env.VITE_APP_VERSION || '1.0.0'
})

const browserInfo = computed(() => {
  const ua = navigator.userAgent
  let browserName = "Unknown"
  if (ua.indexOf("Chrome") > -1) browserName = "Chrome"
  else if (ua.indexOf("Safari") > -1) browserName = "Safari"
  else if (ua.indexOf("Firefox") > -1) browserName = "Firefox"
  return `${browserName} on ${navigator.platform}`
})

/**
 * 处理检查更新
 */
const handleCheckUpdate = async () => {
  checkingUpdate.value = true
  try {
    const versionChecker = getVersionChecker(appVersion.value)
    await versionChecker.manualCheck()
  } finally {
    checkingUpdate.value = false
  }
}

const handleLogout = () => {
  if (confirm('确定要退出登录吗？')) {
      localStorage.removeItem('token')
      // Force reload to clear state effectively or use a global store
      window.location.href = '/'
  }
}

const handleSwitchAccount = () => {
  // Clear token and go to login page
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.settings-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
  animation: fadeIn 0.5s ease;
  color: var(--text);
}

/* CSS Variables Fallback/Definition for this component scope if global ones are missing */
:root {
    --surface: rgba(255, 255, 255, 0.05); /* Dark mode default */
    --border: rgba(255, 255, 255, 0.1);
    --text: #ffffff;
    --muted: rgba(255, 255, 255, 0.6);
    --border-hover: rgba(255, 255, 255, 0.2);
    --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
    --bg: #000;
}

:root[data-theme='light'] {
    --surface: #ffffff;
    --border: #e5e7eb;
    --text: #111827;
    --muted: #6b7280;
    --border-hover: #d1d5db;
    --bg: #f3f4f6;
}

.settings-header {
  margin-bottom: 40px;
  text-align: center;
  
  .page-title {
    font-size: 2.5rem;
    font-weight: 700;
    margin: 0 0 10px;
    background: linear-gradient(135deg, var(--primary), var(--primary-hover));
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  
  .page-subtitle {
    color: var(--muted);
    font-size: 1.1rem;
  }
}

.settings-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.glass-panel {
  background: var(--surface);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 24px;
  transition: all 0.3s ease;
  
  &:hover {
    box-shadow: var(--shadow-md);
    border-color: var(--border-hover);
  }
}

.section-title {
  font-size: 1.2rem;
  font-weight: 600;
  margin: 0 0 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text);
  
  .badge-lock {
    font-size: 0.75rem;
    background: var(--bg);
    padding: 4px 8px;
    border-radius: 12px;
    color: var(--muted);
    font-weight: normal;
  }
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid var(--border);
  
  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
  
  &:first-of-type {
    padding-top: 0;
  }
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  
  .label {
    font-weight: 500;
    color: var(--text);
  }
  
  .description {
    font-size: 0.85rem;
    color: var(--muted);
  }
}

.language-select {
  width: 120px;
}

.login-placeholder {
  text-align: center;
  padding: 30px 0;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 12px;
  
  p {
    color: var(--muted);
    margin-bottom: 16px;
  }
}

.locked {
  opacity: 0.8;
  position: relative;
  overflow: hidden;
}

.text-danger {
  color: #ef4444;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .settings-container {
    padding: max(20px, env(safe-area-inset-top)) 16px 100px 16px;
    min-height: 100%;
    overflow-y: auto;
  }
  
  .page-title {
    font-size: 2rem;
  }
  
  .glass-panel {
    padding: 20px;
  }
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
