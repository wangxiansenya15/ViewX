<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-800 dark:text-white">视频审核</h1>
      <div class="flex gap-3">
        <select 
          v-model="statusFilter" 
          @change="fetchVideos"
          class="px-4 py-2 rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-800 dark:text-white focus:ring-2 focus:ring-indigo-500 outline-none"
        >
          <option value="">全部状态</option>
          <option value="PENDING">待审核</option>
          <option value="APPROVED">已通过</option>
          <option value="REJECTED">已拒绝</option>
        </select>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center py-12 text-gray-500">
      加载中...
    </div>

    <!-- Empty State -->
    <div v-else-if="videos.length === 0" class="text-center py-12 text-gray-500">
      暂无待审核的视频
    </div>

    <!-- Video Grid -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div 
        v-for="video in videos" 
        :key="video.id" 
        class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 overflow-hidden group"
      >
        <!-- Thumbnail -->
        <div class="aspect-video bg-gray-200 dark:bg-gray-700 relative overflow-hidden">
          <img 
            v-if="video.thumbnailUrl || video.coverUrl" 
            :src="video.thumbnailUrl || video.coverUrl" 
            :alt="video.title"
            class="w-full h-full object-cover"
          />
          <div v-else class="absolute inset-0 flex items-center justify-center text-gray-400">
            <span class="text-sm">无封面</span>
          </div>
          <div class="absolute top-2 right-2 px-2 py-1 bg-black/50 text-white text-xs rounded backdrop-blur-sm">
            {{ formatDuration(video.duration) }}
          </div>
          <!-- Status Badge -->
          <div 
            v-if="video.status !== 'PENDING'"
            :class="[
              'absolute top-2 left-2 px-2 py-1 text-xs rounded backdrop-blur-sm font-medium',
              video.status === 'APPROVED' ? 'bg-green-500/80 text-white' : 'bg-red-500/80 text-white'
            ]"
          >
            {{ video.status === 'APPROVED' ? '已通过' : '已拒绝' }}
          </div>
        </div>

        <!-- Content -->
        <div class="p-4">
          <h3 class="font-bold text-gray-800 dark:text-white mb-1 line-clamp-1">{{ video.title }}</h3>
          <p class="text-sm text-gray-500 mb-4 line-clamp-2">{{ video.description || '暂无描述' }}</p>
          
          <div class="flex items-center justify-between text-xs text-gray-500 mb-4">
            <span>上传者: {{ video.uploaderNickname }}</span>
            <span>{{ formatTime(video.createdAt) }}</span>
          </div>

          <!-- Action Buttons (只在待审核状态显示) -->
          <div v-if="video.status === 'PENDING'" class="flex gap-2">
            <button 
              @click="approveVideo(video.id)"
              :disabled="processingIds.has(video.id)"
              class="flex-1 py-2 bg-green-600 hover:bg-green-700 text-white text-sm font-medium rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ processingIds.has(video.id) ? '处理中...' : '通过' }}
            </button>
            <button 
              @click="openRejectModal(video)"
              :disabled="processingIds.has(video.id)"
              class="flex-1 py-2 bg-red-100 hover:bg-red-200 text-red-600 dark:bg-red-900/30 dark:hover:bg-red-900/50 dark:text-red-400 text-sm font-medium rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              拒绝
            </button>
          </div>

          <!-- Status Info (已审核的视频) -->
          <div v-else class="text-center py-2 text-sm text-gray-500">
            {{ video.status === 'APPROVED' ? '审核已通过' : '审核未通过' }}
          </div>
        </div>
      </div>
    </div>

    <!-- Reject Modal -->
    <div v-if="showRejectModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div class="bg-white dark:bg-gray-800 rounded-2xl w-full max-w-md overflow-hidden shadow-2xl">
        <div class="p-6 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center">
          <h3 class="text-xl font-bold text-gray-800 dark:text-white">拒绝视频</h3>
          <button @click="closeRejectModal" class="text-gray-400 hover:text-gray-600 dark:hover:text-white">
            ✕
          </button>
        </div>
        
        <div class="p-6 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">拒绝原因</label>
            <textarea 
              v-model="rejectReason" 
              rows="4" 
              class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-red-500 outline-none" 
              placeholder="请输入拒绝原因..."
            ></textarea>
          </div>
        </div>
        
        <div class="p-6 border-t border-gray-200 dark:border-gray-700 flex justify-end space-x-3">
          <button @click="closeRejectModal" class="px-4 py-2 rounded-lg text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">取消</button>
          <button 
            @click="confirmReject" 
            :disabled="rejecting"
            class="px-4 py-2 rounded-lg bg-red-600 hover:bg-red-700 text-white transition-colors flex items-center disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span v-if="rejecting" class="mr-2 animate-spin">⟳</span>
            {{ rejecting ? '提交中...' : '确认拒绝' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi, type VideoReviewVO } from '@/api'
import { ElMessage } from 'element-plus'

const videos = ref<VideoReviewVO[]>([])
const loading = ref(false)
const statusFilter = ref('PENDING') // 默认显示待审核
const processingIds = ref(new Set<number>())

// Reject Modal State
const showRejectModal = ref(false)
const selectedVideo = ref<VideoReviewVO | null>(null)
const rejectReason = ref('')
const rejecting = ref(false)

// 获取视频列表
const fetchVideos = async () => {
  loading.value = true
  try {
    const status = statusFilter.value || undefined
    const res = status === 'PENDING' 
      ? await adminApi.getPendingVideos(1, 50)
      : await adminApi.getAllVideos(status, 1, 50)
    videos.value = res
  } catch (e) {
    console.error('获取视频列表失败:', e)
    ElMessage.error('获取视频列表失败')
  } finally {
    loading.value = false
  }
}

// 审核通过
const approveVideo = async (videoId: number) => {
  processingIds.value.add(videoId)
  try {
    await adminApi.approveVideo(videoId)
    ElMessage.success('视频审核通过')
    // 从列表中移除或更新状态
    const index = videos.value.findIndex(v => v.id === videoId)
    if (index !== -1) {
      if (statusFilter.value === 'PENDING') {
        videos.value.splice(index, 1)
      } else {
        videos.value[index].status = 'APPROVED'
      }
    }
  } catch (e) {
    console.error('审核通过失败:', e)
    ElMessage.error('审核通过失败')
  } finally {
    processingIds.value.delete(videoId)
  }
}

// 打开拒绝弹窗
const openRejectModal = (video: VideoReviewVO) => {
  selectedVideo.value = video
  rejectReason.value = ''
  showRejectModal.value = true
}

// 关闭拒绝弹窗
const closeRejectModal = () => {
  showRejectModal.value = false
  selectedVideo.value = null
  rejectReason.value = ''
}

// 确认拒绝
const confirmReject = async () => {
  if (!selectedVideo.value) return
  
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }

  rejecting.value = true
  try {
    await adminApi.rejectVideo(selectedVideo.value.id, rejectReason.value)
    ElMessage.success('视频已拒绝')
    
    // 从列表中移除或更新状态
    const index = videos.value.findIndex(v => v.id === selectedVideo.value!.id)
    if (index !== -1) {
      if (statusFilter.value === 'PENDING') {
        videos.value.splice(index, 1)
      } else {
        videos.value[index].status = 'REJECTED'
      }
    }
    
    closeRejectModal()
  } catch (e) {
    console.error('拒绝视频失败:', e)
    ElMessage.error('拒绝视频失败')
  } finally {
    rejecting.value = false
  }
}

// 格式化时长
const formatDuration = (seconds: number) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 格式化时间
const formatTime = (dateStr: string) => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString()
}

onMounted(() => {
  fetchVideos()
})
</script>
