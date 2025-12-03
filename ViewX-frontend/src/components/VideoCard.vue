<template>
  <div class="group flex flex-col gap-3 cursor-pointer" @click="$emit('click', video)">
    <!-- 封面容器 -->
    <div
      class="relative aspect-video rounded-xl overflow-hidden glass-card hover-spring shadow-lg hover:shadow-indigo-500/20">
      <img :src="video.cover"
        class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" loading="lazy">

      <!-- 悬浮播放按钮 -->
      <div
        class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center backdrop-blur-[2px]">
        <div
          class="w-12 h-12 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center border border-white/30">
          <Play class="w-5 h-5 fill-white text-white ml-1" />
        </div>
      </div>

      <!-- 时长标记 -->
      <div
        class="absolute bottom-2 right-2 bg-black/60 backdrop-blur-md text-[10px] px-1.5 py-0.5 rounded text-white font-medium border border-white/10">
        {{ video.duration }}
      </div>
    </div>

    <!-- 信息 -->
    <div class="flex gap-3 px-1">
      <img :src="video.avatar" class="w-9 h-9 rounded-full border border-white/10 bg-gray-800">
      <div class="flex flex-col gap-1">
        <h3
          class="text-sm font-bold text-[var(--text)] leading-tight line-clamp-2 group-hover:text-[var(--primary)] transition-colors">
          {{ video.title }}
        </h3>
        <div class="flex items-center gap-2 text-xs text-[var(--muted)]">
          <span>{{ video.author }}</span>
          <span class="w-0.5 h-0.5 rounded-full bg-[var(--muted)]"></span>
          <span>{{ video.views }}观看</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Play } from 'lucide-vue-next'

interface Video {
  id: number
  cover: string
  title: string
  author: string
  views: string
  duration: string
  avatar: string
  date?: string
}

defineProps<{
  video: Video
}>()

defineEmits(['click'])
</script>