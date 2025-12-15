<template>
  <div class="oauth-callback-container">
    <div class="loading-content">
      <div class="spinner"></div>
      <h2>{{ message }}</h2>
      <p v-if="error" class="error-message">{{ error }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const message = ref('正在处理登录...')
const error = ref('')

onMounted(() => {
  handleOAuthCallback()
})

const handleOAuthCallback = async () => {
  try {
    // 从 URL 参数获取 token
    const token = route.query.token as string
    const errorParam = route.query.error as string

    if (errorParam) {
      // 处理错误情况
      const errorMessages: Record<string, string> = {
        'no_authentication': '未找到认证信息',
        'invalid_auth_type': '无效的认证类型',
        'unsupported_provider': '不支持的登录方式',
        'login_failed': '登录失败，请重试'
      }
      
      error.value = errorMessages[errorParam] || '登录过程中发生错误'
      message.value = '登录失败'
      
      ElMessage.error(error.value)
      
      // 3秒后跳转到登录页
      setTimeout(() => {
        router.push('/login')
      }, 3000)
      return
    }

    if (!token) {
      error.value = '未获取到登录凭证'
      message.value = '登录失败'
      ElMessage.error('登录失败：未获取到 token')
      
      setTimeout(() => {
        router.push('/login')
      }, 3000)
      return
    }

    // 保存 token 到 localStorage
    localStorage.setItem('token', token)
    
    message.value = '登录成功！正在跳转...'
    ElMessage.success('登录成功')

    // 触发自定义事件通知 App.vue 更新登录状态
    window.dispatchEvent(new CustomEvent('auth-state-changed', { 
      detail: { isLoggedIn: true } 
    }))

    // 短暂延迟后跳转，让用户看到成功消息
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // 使用 router.replace 跳转，不会触发页面重载
    router.replace('/')

  } catch (err) {
    console.error('OAuth callback error:', err)
    error.value = '处理登录信息时发生错误'
    message.value = '登录失败'
    ElMessage.error('登录处理失败')
    
    setTimeout(() => {
      router.push('/login')
    }, 3000)
  }
}
</script>

<style scoped>
.oauth-callback-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.loading-content {
  text-align: center;
  padding: 3rem;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  max-width: 400px;
}

.spinner {
  width: 60px;
  height: 60px;
  margin: 0 auto 2rem;
  border: 4px solid rgba(102, 126, 234, 0.2);
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

h2 {
  color: #333;
  font-size: 1.5rem;
  margin-bottom: 1rem;
  font-weight: 600;
}

.error-message {
  color: #e74c3c;
  font-size: 0.95rem;
  margin-top: 1rem;
  padding: 0.75rem;
  background: rgba(231, 76, 60, 0.1);
  border-radius: 8px;
}
</style>
