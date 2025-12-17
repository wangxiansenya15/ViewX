<template>
  <div class="search-results-page p-4 md:p-8 max-w-7xl mx-auto w-full min-h-full">
    <div class="mb-6">
      <h1 class="text-2xl font-bold mb-4">搜索结果: "{{ keyword }}"</h1>
      <!-- Tabs -->
      <div class="flex gap-6 border-b border-[var(--border)]">
        <button 
          @click="activeTab = 'videos'" 
          :class="['pb-3 px-1 font-medium transition-colors border-b-2 text-sm md:text-base', activeTab === 'videos' ? 'border-[var(--primary)] text-[var(--primary)]' : 'border-transparent text-[var(--muted)] hover:text-[var(--text)]']"
        >
          视频
        </button>
        <button 
          @click="activeTab = 'users'" 
          :class="['pb-3 px-1 font-medium transition-colors border-b-2 text-sm md:text-base', activeTab === 'users' ? 'border-[var(--primary)] text-[var(--primary)]' : 'border-transparent text-[var(--muted)] hover:text-[var(--text)]']"
        >
          用户
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center py-20">
       <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-[var(--primary)]"></div>
    </div>

    <!-- Videos Tab -->
    <div v-else-if="activeTab === 'videos'" class="video-grid">
      <div v-if="videos.length === 0" class="text-center py-20 text-[var(--muted)]">
        <div class="text-6xl mb-4">📹</div>
        没有找到与 "{{ keyword }}" 相关的视频
      </div>
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        <div v-for="video in videos" :key="video.id" class="group cursor-pointer flex flex-col gap-2" @click="$router.push(`/video/${video.id}`)">
           <div class="relative aspect-video rounded-xl overflow-hidden bg-[var(--bg-secondary)] shadow-sm group-hover:shadow-md transition-all duration-300">
              <img :src="video.coverUrl || video.thumbnailUrl || '/default-cover.png'" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"/>
              <div class="absolute inset-0 bg-black/10 group-hover:bg-transparent transition-colors"></div>
              <span v-if="video.duration" class="absolute bottom-1.5 right-1.5 bg-black/80 px-1.5 py-0.5 text-xs font-bold rounded text-white tracking-wide">{{ formatDuration(video.duration) }}</span>
           </div>
           <div class="flex gap-3">
              <img :src="video.uploaderAvatar || '/default-avatar.png'" class="w-9 h-9 rounded-full object-cover bg-[var(--bg-secondary)] flex-shrink-0"/>
              <div class="flex-1 min-w-0">
                 <h3 class="font-bold text-sm md:text-[15px] leading-snug mb-1 line-clamp-2 text-[var(--text)] group-hover:text-[var(--primary)] transition-colors">{{ video.title }}</h3>
                 <div class="text-xs text-[var(--muted)] flex flex-wrap items-center gap-1">
                    <span class="hover:text-[var(--text)] transition-colors">{{ video.uploaderNickname }}</span>
                    <span>·</span>
                    <span>{{ formatViewCount(video.viewCount) }}次观看</span>
                    <span>·</span>
                    <span>{{ formatDate(video.publishedAt) }}</span>
                 </div>
                 <div v-if="video.description" class="text-xs text-[var(--muted)] mt-1 line-clamp-1 opacity-70">{{ video.description }}</div>
              </div>
           </div>
        </div>
      </div>
    </div>

    <!-- Users Tab -->
    <div v-else-if="activeTab === 'users'" class="user-list space-y-4 max-w-4xl mx-auto">
       <div v-if="users.length === 0" class="text-center py-20 text-[var(--muted)]">
          <div class="text-6xl mb-4">👥</div>
          没有找到与 "{{ keyword }}" 相关的用户
       </div>
       <div v-else v-for="user in users" :key="user.id" class="flex flex-col sm:flex-row sm:items-center justify-between bg-[var(--bg-card)] p-4 sm:p-5 rounded-2xl border border-[var(--border)] hover:border-[var(--primary)]/30 transition-all cursor-pointer group shadow-sm hover:shadow-md" @click="$router.push(`/profile/${user.id}`)">
          <div class="flex items-center gap-4">
             <div class="relative">
               <img :src="user.avatar || '/default-avatar.png'" class="w-16 h-16 sm:w-20 sm:h-20 rounded-full object-cover border-2 border-[var(--bg)] shadow-md group-hover:scale-105 transition-transform"/>
             </div>
             <div>
                <div class="font-bold text-lg text-[var(--text)] flex items-center gap-2">
                  {{ user.nickname || user.username }}
                </div>
                <div class="text-sm text-[var(--muted)] font-mono">@{{ user.username }}</div>
                <div class="text-sm text-[var(--muted)] mt-1.5 line-clamp-2 max-w-md">{{ user.bio || '这个人很懒，什么都没写' }}</div>
             </div>
          </div>
          <button class="mt-4 sm:mt-0 bg-[var(--primary)] hover:bg-[var(--primary-hover)] text-white px-5 py-2 rounded-full text-sm font-bold transition-all transform active:scale-95 shadow-lg shadow-indigo-500/20">
             访问主页
          </button>
       </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { search, type VideoListVO, type UserSummaryVO } from '@/api'

const route = useRoute()
const keyword = ref('')
const activeTab = ref<'videos' | 'users'>('videos')
const loading = ref(false)
const videos = ref<VideoListVO[]>([])
const users = ref<UserSummaryVO[]>([])

const performSearch = async () => {
  const q = route.query.q as string
  if (!q) {
    keyword.value = ''
    videos.value = []
    users.value = []
    return
  }
  
  keyword.value = q
  loading.value = true
  
  // 重置结果
  videos.value = []
  users.value = []
  
  try {
     // 并行请求
     const [vRes, uRes] = await Promise.all([
        search.searchVideos(q, 1, 50),
        search.searchUsers(q, 1, 50)
     ])
     
     // 简单处理响应，假设响应是数组
     // 如果有分页需求，后续可加
     // 假设 vRes 是 VideoListVO[] 或 Result
     // 在 api.ts 中看返回值是 Promise<VideoListVO[]> (被拦截器解包)
     
     console.log('Search results - videos:', vRes)
     console.log('Search results - users:', uRes)
     
     videos.value = (vRes as any) || []
     users.value = (uRes as any) || []
     
     // 检查用户数据
     if (users.value.length > 0) {
       console.log('First user:', users.value[0])
       console.log('First user ID:', users.value[0].id)
     }
     
  } catch(e) {
     console.error('Search error:', e)
  } finally {
     loading.value = false
  }
}

// 监听 query 变化重新搜索
watch(() => route.query.q, performSearch)

onMounted(performSearch)

// Utils
const formatViewCount = (count?: number) => {
   if (!count) return '0'
   if (count >= 10000) return (count / 10000).toFixed(1) + '万'
   return count.toString()
}

const formatDuration = (seconds?: number) => {
    if (!seconds) return '00:00'
    const h = Math.floor(seconds / 3600)
    const m = Math.floor((seconds % 3600) / 60)
    const s = seconds % 60
    
    if (h > 0) {
      return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
    }
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

const formatDate = (dateStr?: string) => {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    const now = new Date()
    const diff = now.getTime() - date.getTime()
    
    // Simple relative time
    const days = Math.floor(diff / (1000 * 60 * 60 * 24))
    if (days === 0) return '今天'
    if (days === 1) return '昨天'
    if (days < 30) return `${days}天前`
    if (days < 365) return `${Math.floor(days / 30)}个月前`
    return `${Math.floor(days / 365)}年前`
}
</script>

<style scoped>
/* 确保内容在深色模式下有对比度 */
</style>
