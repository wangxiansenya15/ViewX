<template>
  <div class="group flex flex-col gap-3 cursor-pointer" @click="$emit('click', video)">
    <!-- 封面容器 -->
    <div
      class="relative aspect-video rounded-xl overflow-hidden glass-card hover-spring shadow-lg hover:shadow-indigo-500/20">
      <img :src="video.coverUrl || video.thumbnailUrl"
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
        {{ formatDuration(video.duration) }}
      </div>
    </div>

    <!-- 信息 -->
    <!-- 信息 -->
    <div class="flex flex-col gap-1.5 px-0.5">
      <h3
        class="text-sm font-bold text-[var(--text)] leading-snug line-clamp-2 group-hover:text-[var(--primary)] transition-colors" :title="video.title">
        {{ video.title }}
      </h3>
      
      <!-- 视频描述 -->
      <p v-if="video.description" class="text-xs text-[var(--muted)] line-clamp-1" :title="video.description">
          {{ video.description }}
      </p>

      <!-- 用户信息 -->
      <div class="flex items-center gap-2 mt-0.5">
        <img v-if="showAvatar" :src="video.uploaderAvatar" class="w-6 h-6 rounded-full border border-white/10 bg-gray-800 object-cover shrink-0">
        <span v-if="showAvatar" class="text-xs text-[var(--muted)] font-medium hover:text-[var(--text)] transition-colors">{{ video.uploaderNickname }}</span>
        <!-- 只有在不显示头像的模式下（如个人页），且需要显示观看数时才显示观看数？或者根据设计决定。但用户只要名字和描述。-->
      </div>
    </div>
  </div>
</template>


<script setup lang="ts">
import { Play } from 'lucide-vue-next'
import type { VideoVO } from '@/api'

const formatDuration = (seconds: number) => {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

withDefaults(defineProps<{
  video: VideoVO
  showAvatar?: boolean
}>(), {
  showAvatar: true
})

defineEmits(['click'])
</script>