<template>
  <div class="settings-container">
    <div class="settings-header">
      <h1 class="page-title">{{ t('settings.title') }}</h1>
      <p class="page-subtitle">{{ t('settings.subtitle') }}</p>
    </div>

    <div class="settings-content">
      <!-- 基础设置 -->
      <section class="settings-section glass-panel">
        <h2 class="section-title">
          <span class="icon">🎨</span>
          {{ t('settings.appearance.title') }}
        </h2>
        
        <div class="setting-item">
          <div class="setting-info">
            <span class="label">{{ t('settings.appearance.darkMode') }}</span>
            <span class="description">{{ t('settings.appearance.darkModeDesc') }}</span>
          </div>
          <el-switch
            v-model="isDarkMode"
            @change="toggleTheme"
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

      <!-- 关于信息 -->
      <section class="settings-section glass-panel">
        <h2 class="section-title">
          <span class="icon">ℹ️</span>
          {{ t('settings.about.title') }}
        </h2>
        
        <div class="setting-item">
          <div class="setting-info">
            <span class="label">{{ t('settings.about.version') }}</span>
            <span class="description">v1.0.0 (Beta)</span>
          </div>
          <el-button link type="primary">{{ t('settings.about.checkUpdate') }}</el-button>
        </div>
        
        <div class="setting-item">
          <div class="setting-info">
            <span class="label">{{ t('settings.about.browserInfo') }}</span>
            <span class="description">{{ browserInfo }}</span>
          </div>
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
          <el-button type="primary" round @click="$emit('require-login')">{{ t('settings.security.loginBtn') }}</el-button>
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  isLoggedIn: boolean
  theme: 'light' | 'dark'
}>()

const emit = defineEmits<{
  'toggle-theme': []
  'require-login': []
}>()

const { t, locale } = useI18n()

const isDarkMode = computed({
  get: () => props.theme === 'dark',
  set: () => emit('toggle-theme')
})

const twoFactorEnabled = ref(false)

const browserInfo = computed(() => {
  const ua = navigator.userAgent
  let browserName = "Unknown"
  if (ua.indexOf("Chrome") > -1) browserName = "Chrome"
  else if (ua.indexOf("Safari") > -1) browserName = "Safari"
  else if (ua.indexOf("Firefox") > -1) browserName = "Firefox"
  return `${browserName} on ${navigator.platform}`
})

function toggleTheme() {
  // Logic handled by computed setter
}
</script>

<style lang="scss" scoped>
.settings-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
  animation: fadeIn 0.5s ease;
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
    padding: 20px 16px;
  }
  
  .page-title {
    font-size: 2rem;
  }
  
  .glass-panel {
    padding: 20px;
  }
}
</style>
