<template>
  <div class="h-full w-full">
    <!-- Mobile Feed -->
    <div v-if="isMobile" class="absolute inset-0 z-0 bg-black">
      <MobileFeed :videos="feedVideos" @open-comments="openComments" />
    </div>

    <!-- Desktop Masonry -->
    <div v-else class="h-full px-6 py-6 scroll-smooth">
       <VideoMasonry :videos="feedVideos" @open-video="openDesktopVideo" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, inject, type Ref } from 'vue'
import { videoApi, type VideoVO } from '@/api'
import MobileFeed from '@/components/mobile/MobileFeed.vue'
import VideoMasonry from '@/components/VideoMasonry.vue'

// Inject provided keys from App.vue
const isMobile = inject<Ref<boolean>>('isMobile', ref(false))
const openCommentsAction = inject<(v: VideoVO) => void>('openComments')
const openDesktopVideoAction = inject<(v: VideoVO) => void>('openDesktopVideo')

const feedVideos = ref<VideoVO[]>([])


const fetchVideos = async () => {
  try {
     const res = await videoApi.getFeed()
     if (res && res.length > 0) {
       // Fetch full details for each video to get the videoUrl
       const detailPromises = res.map(video => 
         videoApi.getDetail(video.id)
           .then(detail => ({
             ...video,
             // Merge detail fields that might be missing in list VO
             videoUrl: detail.videoUrl, 
             isLiked: detail.isLiked,
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
       
       // Filter valid ones or apply polyfill if really needed (for dev smoothness)
       feedVideos.value = videosWithDetails.map((v, i) => ({
         ...v,
         // Keep polyfill as absolute fallback for dev environment if backend URL is empty/broken
         videoUrl: v.videoUrl || (i % 2 === 0 
           ? 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4' 
           : 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4')
       }))
       
     } else {
       throw new Error('No data')
     }
  } catch (e) {
     console.error('Fetch failed, using mock data:', e)
     // Fallback Mock Data
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
</script>
