<template>
  <div class="space-y-6">
    <h1 class="text-2xl font-bold text-gray-800 dark:text-white">仪表盘</h1>
    
    <!-- Stats Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <div v-for="stat in stats" :key="stat.label" 
        class="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 transition-transform hover:-translate-y-1">
        <div class="flex items-center justify-between mb-4">
          <div :class="`p-3 rounded-lg ${stat.bgClass} ${stat.textClass}`">
            <component :is="stat.icon" class="w-6 h-6" />
          </div>
          <span :class="`text-sm font-medium ${stat.trend > 0 ? 'text-green-500' : 'text-red-500'}`">
            {{ stat.trend > 0 ? '+' : '' }}{{ stat.trend }}%
          </span>
        </div>
        <h3 class="text-gray-500 dark:text-gray-400 text-sm font-medium">{{ stat.label }}</h3>
        <p class="text-2xl font-bold text-gray-800 dark:text-white mt-1">{{ stat.value }}</p>
      </div>
    </div>

    <!-- Recent Activity or Charts could go here -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div class="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
        <h3 class="text-lg font-bold text-gray-800 dark:text-white mb-4">待审核视频</h3>
        <div class="space-y-4">
          <div v-for="i in 3" :key="i" class="flex items-center gap-4 p-3 hover:bg-gray-50 dark:hover:bg-gray-700/50 rounded-lg transition-colors cursor-pointer">
            <div class="w-16 h-10 bg-gray-200 dark:bg-gray-700 rounded-md"></div>
            <div class="flex-1">
              <h4 class="text-sm font-medium text-gray-800 dark:text-white">示例视频标题 {{ i }}</h4>
              <p class="text-xs text-gray-500">上传于 2小时前</p>
            </div>
            <span class="px-2 py-1 text-xs font-medium text-orange-600 bg-orange-50 dark:bg-orange-900/30 dark:text-orange-400 rounded-full">待审核</span>
          </div>
        </div>
      </div>

      <div class="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
        <h3 class="text-lg font-bold text-gray-800 dark:text-white mb-4">系统公告</h3>
        <div class="text-gray-500 dark:text-gray-400 text-sm">
          暂无系统公告
        </div>
      </div>
      </div>

    
    <!-- System Status Section -->
    <div class="space-y-6">
       <h2 class="text-xl font-bold text-gray-800 dark:text-white mt-8">系统监控</h2>
       <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <!-- Health Status -->
          <div class="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
            <h3 class="text-lg font-bold text-gray-800 dark:text-white mb-4">健康状态</h3>
            <div class="flex items-center gap-4 mb-4">
                <div :class="`px-3 py-1 rounded-full text-sm font-medium ${healthStatus === 'UP' ? 'bg-green-100 text-green-600' : 'bg-red-100 text-red-600'}`">
                  {{ healthStatus || '检测中...' }}
                </div>
            </div>
            <div v-if="healthDetails" class="space-y-3">
               <div v-for="(component, name) in healthDetails" :key="name" class="flex justify-between text-sm items-center border-b border-gray-100 dark:border-gray-700 pb-2 last:border-0 last:pb-0">
                  <span class="text-gray-500 capitalize">{{ name }}</span>
                  <span :class="component.status === 'UP' ? 'text-green-500 font-medium' : 'text-red-500 font-medium'">{{ component.status }}</span>
               </div>
            </div>
          </div>
          
          <!-- JVM Metrics -->
          <div class="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
             <h3 class="text-lg font-bold text-gray-800 dark:text-white mb-4">JVM 运行指标</h3>
             <div class="space-y-6">
                <!-- Memory -->
                <div>
                   <div class="flex justify-between text-sm mb-2">
                      <span class="text-gray-500">内存使用 (Heap: {{ formatBytes(memoryUsedBytes) }} / {{ formatBytes(memoryMaxBytes) }})</span>
                      <span class="text-gray-800 dark:text-gray-200 font-medium">{{ memoryUsage }}%</span>
                   </div>
                   <div class="w-full bg-gray-100 rounded-full h-3 dark:bg-gray-700 overflow-hidden">
                      <div class="bg-blue-600 h-full rounded-full transition-all duration-500 ease-out" :style="{ width: memoryUsage + '%' }"></div>
                   </div>
                </div>
                
                <!-- Uptime -->
                <div class="flex justify-between text-sm p-3 bg-gray-50 dark:bg-gray-700/30 rounded-lg">
                   <span class="text-gray-500">运行时长</span>
                   <span class="text-gray-800 dark:text-gray-200 font-mono font-medium">{{ uptime }}</span>
                </div>
                
                <!-- Start Time -->
                <div class="flex justify-between text-sm p-3 bg-gray-50 dark:bg-gray-700/30 rounded-lg">
                   <span class="text-gray-500">启动时间</span>
                   <span class="text-gray-800 dark:text-gray-200 font-mono font-medium">{{ startTime }}</span>
                </div>
             </div>
          </div>
       </div>
       
       <!-- Advanced Monitoring Grid -->
       <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 pb-8">
            <!-- Loggers Management -->
            <div class="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
                <div class="flex justify-between items-center mb-4">
                    <h3 class="text-lg font-bold text-gray-800 dark:text-white">日志级别管理</h3>
                    <div class="flex gap-2 text-xs">
                        <button @click="showAllLoggers = !showAllLoggers" class="px-2 py-1 bg-gray-100 dark:bg-gray-700 rounded hover:bg-gray-200 dark:hover:bg-gray-600">
                            {{ showAllLoggers ? '只看项目日志' : '查看所有日志' }}
                        </button>
                    </div>
                </div>
                <div class="overflow-y-auto max-h-[300px] pr-2 custom-scrollbar">
                    <div v-if="loadingLoggers" class="text-center py-4 text-gray-500">加载日志配置中...</div>
                    <div v-else class="space-y-3">
                        <div v-for="logger in filteredLoggers" :key="logger.name" class="flex justify-between items-center p-2 hover:bg-gray-50 dark:hover:bg-gray-700/30 rounded">
                             <div class="flex-1 min-w-0 pr-4">
                                 <p class="text-sm font-mono text-gray-700 dark:text-gray-300 truncate" :title="logger.name">{{ logger.name }}</p>
                             </div>
                             <div class="flex gap-1">
                                 <button v-for="level in ['DEBUG', 'INFO', 'WARN', 'ERROR']" 
                                         :key="level"
                                         @click="changeLoggerLevel(logger.name, level)"
                                         :class="`px-2 py-0.5 text-xs rounded border transition-colors ${logger.effectiveLevel === level 
                                            ? 'bg-blue-600 text-white border-blue-600' 
                                            : 'border-gray-200 dark:border-gray-600 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700'}`">
                                     {{ level }}
                                 </button>
                             </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Thread & Request Stats -->
            <div class="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
                <h3 class="text-lg font-bold text-gray-800 dark:text-white mb-4">高级指标</h3>
                
                <div class="grid grid-cols-2 gap-4 mb-6">
                    <div class="p-4 bg-gray-50 dark:bg-gray-700/30 rounded-lg">
                        <div class="flex items-center gap-2 mb-2 text-gray-500">
                            <Activity class="w-4 h-4" />
                            <span class="text-sm">活跃线程</span>
                        </div>
                        <p class="text-2xl font-mono text-gray-800 dark:text-white">{{ threadCount }}</p>
                        <p class="text-xs text-gray-400 mt-1">守护线程: {{ daemonThreadCount }}</p>
                    </div>
                    <div class="p-4 bg-gray-50 dark:bg-gray-700/30 rounded-lg">
                         <div class="flex items-center gap-2 mb-2 text-gray-500">
                            <List class="w-4 h-4" />
                            <span class="text-sm">API 映射数</span>
                        </div>
                        <p class="text-2xl font-mono text-gray-800 dark:text-white">{{ mappingCount }}</p>
                        <p class="text-xs text-gray-400 mt-1">Servlet Endpoints</p>
                    </div>
                </div>

                <h4 class="text-sm font-bold text-gray-700 dark:text-gray-300 mb-3">线程状态分布</h4>
                <div class="space-y-2">
                    <div v-for="(count, state) in threadStateDist" :key="state" class="flex items-center justify-between text-xs">
                        <span class="text-gray-500 w-24">{{ state }}</span>
                        <div class="flex-1 mx-2 h-2 bg-gray-100 dark:bg-gray-700 rounded-full overflow-hidden">
                             <div class="h-full bg-indigo-500" :style="{ width: (count / threadCount * 100) + '%' }"></div>
                        </div>
                        <span class="font-mono text-gray-700 dark:text-gray-300">{{ count }}</span>
                    </div>
                </div>
            </div>
       </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Users, Video, Eye, HardDrive, Activity, Settings, List, Server } from 'lucide-vue-next'
import { getSystemHealth, getSystemMetrics, getLoggers, updateLoggerLevel, getThreadDump, getMappings } from '@/api/actuator'

const healthStatus = ref('')
const healthDetails = ref<any>({})
const memoryUsage = ref(0)
const memoryUsedBytes = ref(0)
const memoryMaxBytes = ref(0)
const uptime = ref('')
const startTime = ref('')

const formatBytes = (bytes: number) => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatUptime = (seconds: number) => {
    const d = Math.floor(seconds / (3600 * 24))
    const h = Math.floor((seconds % (3600 * 24)) / 3600)
    const m = Math.floor((seconds % 3600) / 60)
    const parts = []
    if (d > 0) parts.push(`${d}天`)
    if (h > 0) parts.push(`${h}小时`)
    parts.push(`${m}分`)
    return parts.join(' ')
}

// Loggers & Advanced Stats
const loggers = ref<any[]>([])
const showAllLoggers = ref(false)
const loadingLoggers = ref(false)
const threadCount = ref(0)
const daemonThreadCount = ref(0)
const mappingCount = ref(0)
const threadStateDist = ref<Record<string, number>>({})

const filteredLoggers = computed(() => {
    if (showAllLoggers.value) {
        return loggers.value
    }
    // Only show com.flowbrain loggers by default
    return loggers.value.filter(l => l.name.includes('com.flowbrain'))
})

const fetchLoggers = async () => {
    loadingLoggers.value = true
    try {
        const res = (await getLoggers()) as any
        // res.loggers is an object { "name": { "effectiveLevel": "INFO", ... } }
        loggers.value = Object.entries(res.loggers).map(([name, conf]: [string, any]) => ({
            name,
            effectiveLevel: conf.effectiveLevel,
            configuredLevel: conf.configuredLevel
        }))
    } catch (e) {
        console.error('Failed to fetch loggers', e)
    } finally {
        loadingLoggers.value = false
    }
}

const changeLoggerLevel = async (name: string, level: string) => {
    try {
        await updateLoggerLevel(name, level)
        // Optimistic update
        const logger = loggers.value.find(l => l.name === name)
        if (logger) {
            logger.effectiveLevel = level
        }
        ElMessage.success(`已将 ${name} 设置为 ${level}`)
    } catch (e) {
        ElMessage.error('设置日志级别失败')
    }
}

import { adminApi } from '@/api'

// Stats Data
const stats = ref([
  { 
    label: '总用户数', 
    value: '...', 
    trend: 0, 
    icon: Users,
    bgClass: 'bg-blue-50 dark:bg-blue-900/30',
    textClass: 'text-blue-600 dark:text-blue-400'
  },
  { 
    label: '视频总数', 
    value: '...', 
    trend: 0, 
    icon: Video,
    bgClass: 'bg-purple-50 dark:bg-purple-900/30',
    textClass: 'text-purple-600 dark:text-purple-400'
  },
  { 
    label: '今日浏览', // Actually total views but keeping label as per image request
    value: '...', 
    trend: 0, 
    icon: Eye,
    bgClass: 'bg-green-50 dark:bg-green-900/30',
    textClass: 'text-green-600 dark:text-green-400'
  },
  { 
    label: '存储占用', 
    value: '...', 
    trend: 0, 
    icon: HardDrive,
    bgClass: 'bg-orange-50 dark:bg-orange-900/30',
    textClass: 'text-orange-600 dark:text-orange-400'
  },
])

const formatNumber = (num: number) => {
    if (num >= 1000000000) return (num / 1000000000).toFixed(1) + 'B'
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
    if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
    return num.toLocaleString()
}

onMounted(async () => {
    try {
        // Fetch Dashboard Stats
        const res = await adminApi.getDashboardStats()
        if (res) {
            stats.value[0].value = formatNumber(res.totalUsers)
            stats.value[0].trend = res.userTrend
            
            stats.value[1].value = formatNumber(res.totalVideos)
            stats.value[1].trend = res.videoTrend
            
            stats.value[2].value = formatNumber(res.totalViews)
            stats.value[2].trend = res.viewTrend
            
            stats.value[3].value = formatBytes(res.storageUsed)
            stats.value[3].trend = res.storageTrend
        }

        // Fetch Loggers
        fetchLoggers()
        
        // ... (Rest of existing logic)
        
        // Fetch Mappings Count
        const mappingsRes = (await getMappings()) as any
        // spring boot 3 structure: contexts -> application -> mappings -> dispatcherServlets -> dispatcherServlet -> [...]
        let count = 0
        if (mappingsRes.contexts?.application?.mappings?.dispatcherServlets?.dispatcherServlet) {
             count = mappingsRes.contexts.application.mappings.dispatcherServlets.dispatcherServlet.length
        }
        mappingCount.value = count
        
        // Fetch Thread Stats
        const threadDump = (await getThreadDump()) as any
        if (threadDump && threadDump.threads) {
             threadCount.value = threadDump.threads.length
             daemonThreadCount.value = threadDump.threads.filter((t: any) => t.daemon).length
             
             // Calculate distribution
             const dist: Record<string, number> = {}
             threadDump.threads.forEach((t: any) => {
                 dist[t.threadState] = (dist[t.threadState] || 0) + 1
             })
             threadStateDist.value = dist
        }

        // Original Health & JVM Checks
        const health = (await getSystemHealth()) as any
        healthStatus.value = health.status
        healthDetails.value = health.components
        
        const memUsed = (await getSystemMetrics('jvm.memory.used')) as any
        const memMax = (await getSystemMetrics('jvm.memory.max')) as any
        
        if (memUsed && memMax) {
             const used = memUsed.measurements[0].value
             const max = memMax.measurements[0].value
             memoryUsedBytes.value = used
             memoryMaxBytes.value = max
             if (max > 0) {
                 memoryUsage.value = Math.round((used / max) * 100)
             }
        }
        
        const uptimeData = (await getSystemMetrics('process.uptime')) as any
        if (uptimeData) {
            uptime.value = formatUptime(uptimeData.measurements[0].value)
        }
        
        const startTimeData = (await getSystemMetrics('process.start.time')) as any
        if (startTimeData) {
            // value is usually seconds e.g. 1734780000.123
             const timestamp = startTimeData.measurements[0].value * 1000
             startTime.value = new Date(timestamp).toLocaleString()
        }

    } catch (e) {
        console.error('Failed to fetch dashboard data', e)
    }
})


</script>
