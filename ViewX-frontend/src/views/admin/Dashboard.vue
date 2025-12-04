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
  </div>
</template>

<script setup lang="ts">
import { Users, Video, Eye, HardDrive } from 'lucide-vue-next'

const stats = [
  { 
    label: '总用户数', 
    value: '12,345', 
    trend: 12.5, 
    icon: Users,
    bgClass: 'bg-blue-50 dark:bg-blue-900/30',
    textClass: 'text-blue-600 dark:text-blue-400'
  },
  { 
    label: '视频总数', 
    value: '4,567', 
    trend: 8.2, 
    icon: Video,
    bgClass: 'bg-purple-50 dark:bg-purple-900/30',
    textClass: 'text-purple-600 dark:text-purple-400'
  },
  { 
    label: '今日浏览', 
    value: '89.2k', 
    trend: -2.4, 
    icon: Eye,
    bgClass: 'bg-green-50 dark:bg-green-900/30',
    textClass: 'text-green-600 dark:text-green-400'
  },
  { 
    label: '存储占用', 
    value: '1.2 TB', 
    trend: 5.1, 
    icon: HardDrive,
    bgClass: 'bg-orange-50 dark:bg-orange-900/30',
    textClass: 'text-orange-600 dark:text-orange-400'
  },
]
</script>
