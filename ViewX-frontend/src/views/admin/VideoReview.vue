<template>
  <div class="space-y-6">
    <h1 class="text-2xl font-bold text-gray-800 dark:text-white">视频审核</h1>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="i in 6" :key="i" class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 overflow-hidden group">
        <!-- Thumbnail -->
        <div class="aspect-video bg-gray-200 dark:bg-gray-700 relative">
          <div class="absolute inset-0 flex items-center justify-center text-gray-400">
            <span class="text-sm">视频预览 {{ i }}</span>
          </div>
          <div class="absolute top-2 right-2 px-2 py-1 bg-black/50 text-white text-xs rounded backdrop-blur-sm">
            12:34
          </div>
        </div>

        <!-- Content -->
        <div class="p-4">
          <h3 class="font-bold text-gray-800 dark:text-white mb-1 line-clamp-1">待审核视频标题 {{ i }}</h3>
          <p class="text-sm text-gray-500 mb-4 line-clamp-2">这是一个待审核的视频描述内容，审核员需要检查是否包含违规内容。</p>
          
          <div class="flex items-center justify-between text-xs text-gray-500 mb-4">
            <span>上传者: User {{ i }}</span>
            <span>2小时前</span>
          </div>

          <div class="flex gap-2">
            <button class="flex-1 py-2 bg-green-600 hover:bg-green-700 text-white text-sm font-medium rounded-lg transition-colors">
              通过
            </button>
            <button class="flex-1 py-2 bg-red-100 hover:bg-red-200 text-red-600 dark:bg-red-900/30 dark:hover:bg-red-900/50 dark:text-red-400 text-sm font-medium rounded-lg transition-colors">
              拒绝
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// Logic for video review will go here
</script>
