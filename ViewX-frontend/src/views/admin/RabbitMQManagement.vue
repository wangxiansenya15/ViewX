<template>
  <div class="h-full flex flex-col" ref="containerRef">
    <!-- 顶部控制栏 -->
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6 mb-6" :class="{ 'hidden': isFullscreen }">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-800 dark:text-white flex items-center gap-2">
            <el-icon class="text-orange-500"><Connection /></el-icon>
            RabbitMQ 管理控制台
          </h1>
          <p class="text-gray-600 dark:text-gray-400 mt-2">
            消息队列管理和监控
          </p>
        </div>
        <div class="flex items-center gap-3">
          <el-tag type="success" effect="dark">
            <span class="flex items-center gap-1">
              <el-icon><CircleCheck /></el-icon>
              已连接
            </span>
          </el-tag>
          <el-button 
            type="primary" 
            :icon="RefreshRight" 
            @click="refreshIframe"
            circle
            title="刷新"
          />
          <el-button 
            :icon="Maximize2" 
            @click="toggleFullscreen"
            circle
            title="全屏"
          />
        </div>
      </div>
    </div>

    <!-- iframe 容器 -->
    <div 
      ref="iframeContainerRef"
      class="flex-1 bg-white dark:bg-gray-800 rounded-lg shadow-sm overflow-hidden relative"
      :class="{ 'fullscreen-container': isFullscreen }"
    >
      <!-- 全屏模式下的顶部控制栏 -->
      <div 
        v-if="isFullscreen" 
        class="absolute top-0 left-0 right-0 z-50 bg-gray-900/90 backdrop-blur-sm px-6 py-3 flex items-center justify-between"
      >
        <div class="flex items-center gap-3 text-white">
          <el-icon class="text-orange-500"><Connection /></el-icon>
          <span class="font-semibold">RabbitMQ 管理控制台</span>
          <el-tag type="success" size="small" effect="dark">
            <span class="flex items-center gap-1">
              <el-icon><CircleCheck /></el-icon>
              已连接
            </span>
          </el-tag>
        </div>
        <div class="flex items-center gap-3">
          <el-button 
            type="primary" 
            :icon="RefreshRight" 
            @click="refreshIframe"
            circle
            size="small"
            title="刷新"
          />
          <el-button 
            :icon="Minimize2" 
            @click="toggleFullscreen"
            circle
            size="small"
            title="退出全屏"
          />
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="absolute inset-0 flex items-center justify-center bg-white dark:bg-gray-800 z-10">
        <div class="text-center">
          <el-icon class="text-4xl text-indigo-500 animate-spin mb-4"><Loading /></el-icon>
          <p class="text-gray-600 dark:text-gray-400">正在加载 RabbitMQ 管理控制台...</p>
        </div>
      </div>
      
      <!-- RabbitMQ iframe -->
      <iframe
        ref="iframeRef"
        :src="rabbitmqUrl"
        @load="handleIframeLoad"
        class="w-full h-full border-0"
        :class="{ 'opacity-0': loading, 'fullscreen-iframe': isFullscreen }"
        title="RabbitMQ Management Console"
      />
    </div>

    <!-- 提示信息（非全屏时显示） -->
    <div 
      class="mt-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4"
      :class="{ 'hidden': isFullscreen }"
    >
      <div class="flex items-start gap-3">
        <el-icon class="text-blue-500 mt-0.5"><InfoFilled /></el-icon>
        <div class="flex-1">
          <h3 class="font-semibold text-blue-800 dark:text-blue-300 mb-1">使用说明</h3>
          <ul class="text-sm text-blue-700 dark:text-blue-400 space-y-1">
            <li>• 默认用户名: <code class="bg-blue-100 dark:bg-blue-900 px-2 py-0.5 rounded">guest</code></li>
            <li>• 默认密码: <code class="bg-blue-100 dark:bg-blue-900 px-2 py-0.5 rounded">guest</code></li>
            <li>• 管理地址: <code class="bg-blue-100 dark:bg-blue-900 px-2 py-0.5 rounded">{{ rabbitmqUrl }}</code></li>
            <li>• 如果无法加载，请确保 RabbitMQ Management 插件已启用</li>
            <li>• 点击全屏按钮可以全屏查看管理界面</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Maximize2, Minimize2 } from 'lucide-vue-next'
import { 
  Connection, 
  CircleCheck, 
  RefreshRight, 
  Loading,
  InfoFilled
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// RabbitMQ Management URL
const rabbitmqUrl = ref('http://localhost:15672/')
const iframeRef = ref<HTMLIFrameElement | null>(null)
const iframeContainerRef = ref<HTMLDivElement | null>(null)
const loading = ref(true)
const isFullscreen = ref(false)

// 处理 iframe 加载完成
const handleIframeLoad = () => {
  loading.value = false
}

// 刷新 iframe
const refreshIframe = () => {
  if (iframeRef.value) {
    loading.value = true
    iframeRef.value.src = iframeRef.value.src
    ElMessage.success('正在刷新...')
  }
}

// 切换全屏
const toggleFullscreen = () => {
  const container = iframeContainerRef.value
  if (!container) return

  if (!document.fullscreenElement) {
    // 进入全屏
    container.requestFullscreen().then(() => {
      isFullscreen.value = true
      ElMessage.success('已进入全屏模式，按 ESC 键退出')
    }).catch(err => {
      ElMessage.error('无法进入全屏模式')
      console.error('全屏错误:', err)
    })
  } else {
    // 退出全屏
    document.exitFullscreen().then(() => {
      isFullscreen.value = false
      ElMessage.success('已退出全屏模式')
    })
  }
}

// 监听全屏变化（包括 ESC 键退出）
const handleFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
  
  if (!document.fullscreenElement) {
    // 用户通过 ESC 键退出了全屏
    console.log('全屏模式已退出')
  }
}

// 监听键盘事件（ESC 键退出全屏）
const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && isFullscreen.value) {
    toggleFullscreen()
  }
}

onMounted(() => {
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  document.addEventListener('keydown', handleKeyDown)
  
  // 检查 RabbitMQ 是否可访问
  fetch(rabbitmqUrl.value)
    .catch(() => {
      ElMessage.warning({
        message: 'RabbitMQ Management 可能未启动，请检查服务状态',
        duration: 5000
      })
    })
})

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('keydown', handleKeyDown)
  
  // 如果组件卸载时还在全屏，退出全屏
  if (document.fullscreenElement) {
    document.exitFullscreen()
  }
})
</script>

<style scoped>
code {
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

iframe {
  transition: opacity 0.3s ease;
}

/* 全屏容器样式 */
.fullscreen-container {
  background: #f5f5f5 !important;
}

.fullscreen-container:fullscreen {
  width: 100vw !important;
  height: 100vh !important;
  background: #f5f5f5 !important;
  padding: 0 !important;
  margin: 0 !important;
  border-radius: 0 !important;
}

.fullscreen-container:fullscreen .fullscreen-iframe {
  width: 100% !important;
  height: 100% !important;
  position: absolute !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
}

/* Firefox 兼容 */
.fullscreen-container:-moz-full-screen {
  width: 100vw !important;
  height: 100vh !important;
  background: #f5f5f5 !important;
}

/* Webkit 兼容 */
.fullscreen-container:-webkit-full-screen {
  width: 100vw !important;
  height: 100vh !important;
  background: #f5f5f5 !important;
}

/* 暗色模式适配 */
@media (prefers-color-scheme: dark) {
  .fullscreen-container {
    background: #2d2d2d !important;
  }
  
  .fullscreen-container:fullscreen {
    background: #2d2d2d !important;
  }
  
  .fullscreen-container:-moz-full-screen {
    background: #2d2d2d !important;
  }
  
  .fullscreen-container:-webkit-full-screen {
    background: #2d2d2d !important;
  }
}
</style>
