<template>
  <div class="h-full w-full">
    <!-- Mobile Feed -->
    <div v-if="isMobile" class="absolute inset-0 z-0 bg-black transition-opacity duration-300" :style="{ opacity: feedOpacity }">
      <MobileFeed ref="mobileFeedRef" :videos="feedVideos" @open-comments="openComments" @load-more="handleLoadMore" />
    </div>

    <!-- Desktop View -->
    <div v-else class="h-full w-full">
       <!-- Grid Mode -->
       <div v-if="viewMode === 'grid'" class="h-full w-full px-6 py-6 overflow-y-auto scroll-smooth">
           <VideoMasonry :videos="feedVideos" @open-video="openDesktopVideo" />
       </div>
       
       <!-- Feed Mode -->
       <div v-else class="h-full w-full transition-opacity duration-300" :style="{ opacity: feedOpacity }">
           <DesktopFeed ref="desktopFeedRef" :videos="feedVideos" @load-more="handleLoadMore" />
       </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated, onDeactivated, inject, type Ref } from 'vue'
import { videoApi, type VideoVO } from '@/api'
import MobileFeed from '@/components/mobile/MobileFeed.vue'
import VideoMasonry from '@/components/VideoMasonry.vue'
import DesktopFeed from '@/components/desktop/DesktopFeed.vue'
import { useHomeViewMode } from '@/composables/useHomeViewMode'

// 定义组件名称,用于 keep-alive
defineOptions({
  name: 'home'
})

// Inject provided keys from App.vue
const isMobile = inject<Ref<boolean>>('isMobile', ref(false))
const openCommentsAction = inject<(v: VideoVO) => void>('openComments')
const openDesktopVideoAction = inject<(v: VideoVO) => void>('openDesktopVideo')

// Feed refs
const mobileFeedRef = ref<InstanceType<typeof MobileFeed> | null>(null)
const desktopFeedRef = ref<InstanceType<typeof DesktopFeed> | null>(null)
const feedOpacity = ref(1)

// View Mode
const { viewMode } = useHomeViewMode()

const feedVideos = ref<VideoVO[]>([])
const page = ref(1)
const loading = ref(false)
const hasMore = ref(true)


const fetchVideos = async (isLoadMore = false) => {
  if (loading.value || (!hasMore.value && isLoadMore)) {
    console.log('[HomeView] Fetch blocked - loading:', loading.value, 'hasMore:', hasMore.value, 'isLoadMore:', isLoadMore)
    return
  }
  
  console.log('[HomeView] Fetching videos - page:', page.value, 'isLoadMore:', isLoadMore)
  loading.value = true
  try {
     const res = await videoApi.getFeed(page.value)
     console.log('[HomeView] API response:', res?.length, 'videos')
     if (res && res.length > 0) {
       // Fetch full details for each video to get the videoUrl
       const detailPromises = res.map(video => 
         videoApi.getDetail(video.id)
           .then(detail => ({
             ...video,
             // Merge detail fields that might be missing in list VO
             videoUrl: detail.videoUrl, 
             isLiked: detail.isLiked,
             isFavorited: detail.isFavorited,
             tags: detail.tags,
             description: detail.description
           }))
           .catch(err => {
             console.warn(`Failed to fetch detail for video ${video.id}`, err)
             return {
                 ...video,
                 videoUrl: '', // Will be polyfilled or empty
                 isLiked: false
             }
           })
       )
       
       const videosWithDetails = await Promise.all(detailPromises)
       
       console.log('[HomeView] Videos with details:', videosWithDetails.map(v => ({ 
         id: v.id, 
         title: v.title, 
         videoUrl: v.videoUrl 
       })))
       
       // Filter valid ones or apply polyfill if really needed (for dev smoothness)
       const newVideos = videosWithDetails.map((v) => ({
         ...v,
         // Keep polyfill as absolute fallback for dev environment if backend URL is empty/broken
         videoUrl: v.videoUrl || 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4'
       }))
       
       console.log('[HomeView] Final videos after polyfill:', newVideos.map(v => ({ 
         id: v.id, 
         title: v.title, 
         videoUrl: v.videoUrl 
       })))

       if (isLoadMore) {
         console.log('[HomeView] Appending', newVideos.length, 'videos. Total now:', feedVideos.value.length + newVideos.length)
         feedVideos.value.push(...newVideos)
       } else {
         console.log('[HomeView] Setting initial videos:', newVideos.length)
         feedVideos.value = newVideos
       }
       
       if (res.length < 10) {
         console.log('[HomeView] No more videos - received', res.length, '< 10')
         hasMore.value = false
       }
     } else {
       hasMore.value = false
       if (!isLoadMore && feedVideos.value.length === 0) {
          throw new Error('No data')
       }
     }
  } catch (e) {
     console.error('Fetch failed, using mock data:', e)
     // Fallback Mock Data only on first load failure
     if (!isLoadMore && feedVideos.value.length === 0) {
        feedVideos.value = [
          {
            id: 101,
            title: 'Amazing Nature',
            description: 'Beautiful landscapes from around the world.',
            videoUrl: 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4',
            thumbnailUrl: 'https://images.unsplash.com/photo-1472214103451-9374bd1c798e?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60',
            uploaderNickname: 'NatureLover',
            uploaderAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Nature',
            likeCount: 1200,
            commentCount: 45,
            tags: ['Nature', 'Travel'],
            duration: 60,
            viewCount: 5000,
            publishedAt: new Date().toISOString(),
            uploaderId: 1
          }
        ]
     }
  } finally {
    loading.value = false
  }
}

const handleLoadMore = () => {
  console.log('[HomeView] handleLoadMore called - loading:', loading.value, 'hasMore:', hasMore.value, 'currentPage:', page.value)
  if (!loading.value && hasMore.value) {
    page.value++
    console.log('[HomeView] Loading page:', page.value)
    fetchVideos(true)
  }
}

const openComments = (video: VideoVO) => {
   if (openCommentsAction) openCommentsAction(video)
}

const openDesktopVideo = (video: VideoVO) => {
   if (openDesktopVideoAction) openDesktopVideoAction(video)
}

onMounted(() => {
  fetchVideos()
})

// Keep-alive 生命周期钩子
onActivated(() => {
  console.log('🟢🟢🟢 [HomeView] Component ACTIVATED 🟢🟢🟢')
  console.log('🟢 [HomeView] isMobile:', isMobile.value, 'viewMode:', viewMode.value)
  
  // 先设置为透明，防止闪烁
  feedOpacity.value = 0
  
  if (isMobile.value) {
    if (mobileFeedRef.value) {
      console.log('🟢 [HomeView] Scheduling MobileFeed restore...')
      // 使用 requestAnimationFrame 确保在下一帧（DOM 更新后）执行
      requestAnimationFrame(() => {
        setTimeout(() => {
          if (mobileFeedRef.value) {
             mobileFeedRef.value.restoreScrollPosition()
             // 恢复位置后，恢复透明度
             requestAnimationFrame(() => {
               feedOpacity.value = 1
             })
          } else {
             feedOpacity.value = 1 // Fallback
          }
        }, 100)
      })
    } else {
      feedOpacity.value = 1
    }
  } else {
    // PC Ref Logic
    if (viewMode.value === 'feed') {
      console.log('🟢 [HomeView] Scheduling DesktopFeed restore...')
      requestAnimationFrame(() => {
        setTimeout(() => {
           if (desktopFeedRef.value) {
             desktopFeedRef.value.restoreScrollPosition()
             requestAnimationFrame(() => {
               feedOpacity.value = 1
             })
           } else {
             console.warn('🟢 [HomeView] DesktopFeed ref missing for restore')
             feedOpacity.value = 1
           }
        }, 150)
      })
    } else {
      feedOpacity.value = 1
    }
  }
})

onDeactivated(() => {
  console.log('🔴🔴🔴 [HomeView] Component DEACTIVATED 🔴🔴🔴')
  
  // 离开时将透明度设为0，以便回来时从隐藏状态开始
  feedOpacity.value = 0
  
  if (isMobile.value) {
    if (mobileFeedRef.value) {
      mobileFeedRef.value.saveScrollPosition()
    }
  } else {
    // Only save if in feed mode and ref exists
    if (viewMode.value === 'feed' && desktopFeedRef.value) {
      desktopFeedRef.value.saveScrollPosition()
    }
  }
})
</script>
