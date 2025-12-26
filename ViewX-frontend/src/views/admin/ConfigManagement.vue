<template>
  <div class="space-y-6">
    <!-- 密码验证界面 -->
    <div v-if="!isPasswordVerified" class="flex items-center justify-center min-h-[60vh]">
      <div class="w-full max-w-md">
        <div class="bg-white dark:bg-gray-800 p-8 rounded-xl shadow-lg border border-gray-200 dark:border-gray-700">
          <div class="text-center mb-6">
            <div class="inline-flex items-center justify-center w-16 h-16 bg-amber-100 dark:bg-amber-900/30 rounded-full mb-4">
              <Lock class="w-8 h-8 text-amber-600 dark:text-amber-400" />
            </div>
            <h2 class="text-2xl font-bold text-gray-800 dark:text-white">配置管理验证</h2>
            <p class="text-sm text-gray-500 dark:text-gray-400 mt-2">
              此操作需要额外的安全验证
            </p>
          </div>

          <form @submit.prevent="handlePasswordVerify" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                请输入配置管理密码
              </label>
              <input
                v-model="password"
                type="password"
                class="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                placeholder="输入密码"
                :disabled="verifying"
              />
            </div>

            <button
              type="submit"
              :disabled="!password || verifying"
              class="w-full px-4 py-3 bg-gradient-to-r from-blue-500 to-indigo-600 text-white rounded-lg hover:shadow-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed font-medium"
            >
              {{ verifying ? '验证中...' : '验证并进入' }}
            </button>
          </form>

          <div class="mt-4 p-3 bg-amber-50 dark:bg-amber-900/20 rounded-lg border border-amber-200 dark:border-amber-800">
            <p class="text-xs text-amber-700 dark:text-amber-400 flex items-start gap-2">
              <AlertTriangle class="w-4 h-4 flex-shrink-0 mt-0.5" />
              <span>配置管理涉及敏感操作，请妥善保管密码。默认密码请查看 .env 文件中的 CONFIG_ADMIN_PASSWORD</span>
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- 配置管理主界面 -->
    <div v-else>
    <div class="flex justify-between items-center pb-4 border-b border-gray-200 dark:border-gray-700">
      <h1 class="text-2xl font-bold text-gray-800 dark:text-white">配置管理</h1>
      <div class="flex gap-3">
        <button 
          @click="quickUpdateMailPassword" 
          :disabled="loading"
          class="px-4 py-2 bg-gradient-to-r from-blue-500 to-indigo-600 text-white rounded-lg hover:shadow-lg transition-all disabled:opacity-50 text-sm font-medium"
        >
          快速更新邮箱授权码
        </button>
        <button 
          @click="handleRestart" 
          :disabled="loading"
          class="px-4 py-2 bg-gradient-to-r from-amber-500 to-orange-600 text-white rounded-lg hover:shadow-lg transition-all disabled:opacity-50 text-sm font-medium flex items-center gap-2"
        >
          <RotateCcw class="w-4 h-4" />
          重启应用
        </button>
        <button 
          @click="handleBackup" 
          :disabled="loading"
          class="px-4 py-2 bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-200 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-all disabled:opacity-50 text-sm font-medium"
        >
          备份配置
        </button>
        <button 
          @click="loadConfigs" 
          :disabled="loading"
          class="px-4 py-2 bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-200 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-all disabled:opacity-50 text-sm font-medium"
        >
          刷新
        </button>
      </div>
    </div>

    <!-- 警告提示 -->
    <div class="bg-amber-50 dark:bg-amber-900/20 border-l-4 border-amber-500 p-4 rounded-r-lg">
      <div class="flex">
        <div class="flex-shrink-0">
          <AlertTriangle class="h-5 w-5 text-amber-500" />
        </div>
        <div class="ml-3">
          <p class="text-sm text-amber-700 dark:text-amber-400">
            <span class="font-bold">重要提示：</span>
            修改配置后，必须 <span class="font-bold underline cursor-pointer" @click="handleRestart">重启应用</span> 才能生效！
          </p>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading && Object.keys(configs).length === 0" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
    </div>

    <!-- Empty State -->
    <div v-if="!loading && Object.keys(configs).length === 0" class="text-center py-12 text-gray-500">
      暂无配置数据
    </div>

    <!-- Config List -->
    <div class="space-y-4">
      <div v-for="(value, key) in configs" :key="key" 
        class="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 transition-all hover:border-blue-200 dark:hover:border-blue-900"
      >
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div class="flex-1">
            <div class="flex items-center gap-3">
              <h3 class="text-lg font-mono font-semibold text-gray-800 dark:text-gray-200">{{ key }}</h3>
              <span v-if="isSensitive(String(key))" class="px-2 py-0.5 text-xs font-medium bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400 rounded">
                敏感
              </span>
            </div>
            <p class="text-sm text-gray-500 mt-1">{{ configDescriptions[String(key)] || '配置项' }}</p>
          </div>

          <div class="flex-1 md:max-w-xl">
            <!-- Edit Mode -->
            <div v-if="editingKey === key" class="flex flex-col gap-3">
              <input 
                :type="isSensitive(String(key)) ? 'password' : 'text'"
                v-model="editValue"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white font-mono text-sm"
                placeholder="请输入新值"
              />
              <div class="flex justify-end gap-2">
                <button 
                  @click="cancelEdit" 
                  class="px-3 py-1.5 text-sm font-medium text-gray-600 bg-gray-100 hover:bg-gray-200 rounded-md transition-colors"
                >
                  取消
                </button>
                <button 
                  @click="saveConfig(String(key))" 
                  class="px-3 py-1.5 text-sm font-medium text-white bg-green-600 hover:bg-green-700 rounded-md transition-colors"
                >
                  保存
                </button>
              </div>
            </div>

            <!-- View Mode -->
            <div v-else class="flex items-center justify-between gap-4 bg-gray-50 dark:bg-gray-900/50 p-3 rounded-lg border border-gray-100 dark:border-gray-700">
              <code class="font-mono text-sm text-gray-700 dark:text-gray-300 break-all">
                {{ maskValue(String(key), String(value)) }}
              </code>
              <button 
                @click="startEdit(String(key), String(value))" 
                class="flex-shrink-0 px-3 py-1.5 text-sm font-medium text-blue-600 bg-blue-50 hover:bg-blue-100 dark:bg-blue-900/30 dark:text-blue-400 dark:hover:bg-blue-900/50 rounded-md transition-colors"
              >
                编辑
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { AlertTriangle, RotateCcw, Lock } from 'lucide-vue-next'
import {
  verifyConfigPassword,
  getEditableConfigKeys,
  updateConfig,
  updateMailPassword,
  backupConfig,
  restartApplication,
} from '@/api/config'

// Types
type Configs = Record<string, string>

// State
const configs = ref<Configs>({})
const loading = ref(false)
const editingKey = ref<string | null>(null)
const editValue = ref('')

// 密码验证相关状态
const isPasswordVerified = ref(false)
const password = ref('')
const verifying = ref(false)

// Constants
const sensitiveKeys = ['PASSWORD', 'SECRET', 'KEY']
const configDescriptions: Record<string, string> = {
  MAIL_USERNAME: 'QQ邮箱账号',
  MAIL_PASSWORD: 'QQ邮箱授权码（非登录密码）',
  GITHUB_CLIENT_ID: 'GitHub OAuth Client ID',
  GITHUB_CLIENT_SECRET: 'GitHub OAuth Client Secret',
  AI_API_KEY: '阿里云 DashScope API Key',
  DB_USERNAME: '数据库用户名',
  DB_PASSWORD: '数据库密码',
  RABBITMQ_HOST: 'RabbitMQ 主机地址',
  RABBITMQ_PORT: 'RabbitMQ 端口',
  RABBITMQ_USERNAME: 'RabbitMQ 用户名',
  RABBITMQ_PASSWORD: 'RabbitMQ 密码',
  FILE_STORAGE_BASE_URL: '文件存储基础URL',
  FILE_STORAGE_UPLOAD_DIR: '文件上传目录',
}

// 密码验证
const handlePasswordVerify = async () => {
  if (!password.value.trim()) {
    ElMessage.warning('请输入密码')
    return
  }

  verifying.value = true
  try {
    await verifyConfigPassword(password.value)
    ElMessage.success('验证成功')
    isPasswordVerified.value = true
    // 验证成功后加载配置
    loadConfigs()
  } catch (error) {
    ElMessage.error('密码错误，请重试')
    password.value = ''
  } finally {
    verifying.value = false
  }
}

// Helpers
const isSensitive = (_key: string) => {
  // 现在所有允许编辑的配置都视为敏感或只写，不回显值
  return true
}

const maskValue = (_key: string, _value: string) => {
  return '******** (安全保护，不可查看)'
}

// Actions
const loadConfigs = async () => {
  loading.value = true
  try {
    // request 拦截器已经解包，直接返回 data (string[])
    const keys = await getEditableConfigKeys() as string[]
    
    // 构建配置对象
    const newConfigs: Configs = {}
    if (Array.isArray(keys)) {
      keys.forEach((key: string) => {
        newConfigs[key] = '' // 不存储任何值
      })
      configs.value = newConfigs
    } else {
      ElMessage.error('加载配置列表失败：格式错误')
    }
  } catch (error) {
    ElMessage.error('加载配置失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const startEdit = (key: string, value: string) => {
  editingKey.value = key
  editValue.value = value
}

const cancelEdit = () => {
  editingKey.value = null
  editValue.value = ''
}

const saveConfig = async (key: string) => {
  if (!editValue.value.trim()) {
    ElMessage.warning('配置值不能为空')
    return
  }

  loading.value = true
  try {
    // request 拦截器已处理 code，成功直接返回 data，失败会 reject
    await updateConfig(key, editValue.value)
    ElMessage.success('配置更新成功，请重启应用使配置生效')
    configs.value[key] = editValue.value
    cancelEdit()
  } catch (error) {
    // 拦截器已显示错误消息
    console.error('配置更新失败', error)
  } finally {
    loading.value = false
  }
}

const quickUpdateMailPassword = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的QQ邮箱授权码', '快捷更新', {
      confirmButtonText: '更新',
      cancelButtonText: '取消',
      inputType: 'text',
    })
    
    if (!value) return

    loading.value = true
    await updateMailPassword(value)
    ElMessage.success('邮箱授权码更新成功，请重启应用')
    loadConfigs()
  } catch (error) {
    // User canceled or API error (拦截器已显示错误)
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleBackup = async () => {
  try {
    await ElMessageBox.confirm('确认备份当前配置？', '提示', {
      confirmButtonText: '备份',
      cancelButtonText: '取消',
      type: 'info',
    })

    loading.value = true
    await backupConfig()
    ElMessage.success('配置备份成功')
  } catch (error) {
    // User canceled or API error
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleRestart = async () => {
  try {
    await ElMessageBox.confirm(
      '确认重启应用？<br/><br/>应用将在3秒后重启，期间可能无法访问。',
      '重启确认',
      {
        confirmButtonText: '确认重启',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true,
      }
    )

    loading.value = true
    await restartApplication()
    ElMessage.success('应用正在重启，请等待30秒后刷新页面...')
    // 30秒后自动刷新页面
    setTimeout(() => {
      window.location.reload()
    }, 30000)
  } catch (error) {
    // User canceled or API error
    console.error(error)
    loading.value = false
  }
}

// Lifecycle
onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
/* Scoped styles if needed */
</style>
