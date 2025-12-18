<template>
  <div class="h-full flex flex-col bg-[#161616] border-l border-white/10">
    <!-- Header -->
    <div class="p-4 border-b border-white/10 flex items-center justify-between">
       <div class="text-sm font-bold text-white">
         评论 <span class="text-gray-500 font-normal text-xs ml-1">{{ totalComments > 0 ? formatNumber(totalComments) : '' }}</span>
       </div>
       <button @click="$emit('close')" class="hover:bg-white/10 p-1 rounded-full transition-colors">
          <X class="w-5 h-5 text-gray-500" />
       </button>
    </div>

    <!-- Comments List -->
    <div ref="listRef" class="flex-1 overflow-y-auto p-4 space-y-6 scrollbar-thin scrollbar-thumb-white/20 hover:scrollbar-thumb-white/30">
       <div v-if="loading" class="text-center py-8 text-gray-500 text-sm">加载评论...</div>
       <div v-else-if="comments.length === 0" class="text-center py-8 text-gray-500 text-sm">暂无评论，快来抢沙发</div>
       
       <div v-else v-for="comment in comments" :key="comment.id" class="flex gap-3">
          <!-- Avatar -->
          <div class="w-9 h-9 rounded-full overflow-hidden shrink-0 border border-white/10">
             <img :src="comment.avatar" class="w-full h-full object-cover" />
          </div>
          
          <div class="flex-1">
             <!-- User Info -->
             <div class="text-xs text-gray-400 font-bold mb-1">{{ comment.nickname || comment.username }}</div>
             
             <!-- Content -->
             <div class="text-sm text-gray-200 leading-relaxed whitespace-pre-wrap">{{ comment.content }}</div>
             
             <!-- Meta Info & Reply Button -->
             <div class="flex items-center gap-4 mt-2 text-xs text-gray-500">
                <span>{{ formatTime(comment.createdAt) }}</span>
                <button @click="startReply(comment)" class="font-bold hover:text-white transition-colors">回复</button>
             </div>

             <!-- Render Replies (Simple nested for now) -->
             <div v-if="comment.replies && comment.replies.length > 0" class="mt-3 space-y-3 pl-2 border-l-2 border-white/10">
                 <div v-for="reply in comment.replies" :key="reply.id" class="flex gap-2">
                     <div class="w-6 h-6 rounded-full overflow-hidden shrink-0">
                         <img :src="reply.avatar" class="w-full h-full object-cover" />
                     </div>
                     <div class="flex-1">
                         <div class="text-xs text-gray-400 font-bold mb-0.5">{{ reply.nickname || reply.username }}</div>
                         <div class="text-sm text-gray-300">{{ reply.content }}</div>
                         <div class="flex items-center gap-3 mt-1 text-[10px] text-gray-500">
                             <span>{{ formatTime(reply.createdAt) }}</span>
                         </div>
                     </div>
                 </div>
             </div>
          </div>
          
          <!-- Like Button (前端模拟，后端 API 开发中) -->
          <div class="flex flex-col items-center gap-1 pt-1">
             <Heart 
               @click="toggleCommentLike(comment)" 
               class="w-4 h-4 cursor-pointer transition-colors" 
               :class="comment.isLiked ? 'text-red-500 fill-red-500' : 'text-gray-500 hover:text-red-500'"
             />
             <span class="text-[10px] text-gray-500">{{ comment.likeCount || 0 }}</span>
          </div>
       </div>
    </div>

    <!-- Input -->
    <div class="p-4 border-t border-white/10 bg-[#1f1f1f]">
       <!-- Reply Indicator -->
       <div v-if="replyingTo" class="mb-2 flex items-center justify-between text-xs text-gray-400 bg-white/5 px-3 py-1.5 rounded-lg">
           <span>回复 @{{ replyingTo.nickname || replyingTo.username }}:</span>
           <button @click="cancelReply" class="hover:text-white"><X class="w-3 h-3" /></button>
       </div>

       <div class="flex items-center gap-3">
           <div class="w-8 h-8 rounded-full bg-gray-700 shrink-0 overflow-hidden">
               <!-- Placeholder for current user avatar, or fetch from store -->
               <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=User" class="w-full h-full object-cover" />
           </div>
           
           <div class="flex-1 bg-white/5 rounded-full px-4 py-2 flex items-center transition-colors focus-within:bg-white/10">
              <input 
                ref="inputRef"
                v-model="inputContent"
                type="text" 
                :placeholder="replyingTo ? '回复评论...' : '留下你的精彩评论...'" 
                class="bg-transparent border-none text-sm text-white placeholder-gray-500 w-full focus:outline-none"
                @keyup.enter="submitComment"
              />
           </div>
           
           <button 
             @click="submitComment" 
             :disabled="!inputContent.trim() || submitting || !videoId"
             class="p-2 text-indigo-500 font-bold hover:bg-white/5 rounded-full disabled:opacity-50 disabled:cursor-not-allowed transition-all"
             :title="getButtonTitle"
           >
              <Send class="w-5 h-5" :class="{'animate-pulse': submitting}" />
           </button>
       </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, computed } from 'vue'
import { X, Heart, Send } from 'lucide-vue-next'
import { commentApi, type CommentVO } from '@/api'
import { ElMessage } from 'element-plus'

const props = defineProps<{
    videoId: number
}>()

const emit = defineEmits(['close'])

const comments = ref<CommentVO[]>([])
const loading = ref(false)
const submitting = ref(false)
const inputContent = ref('')
const replyingTo = ref<CommentVO | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)
const listRef = ref<HTMLElement | null>(null)

const totalComments = ref(0) // 需要后端返回，目前暂时计算列表长度

const getButtonTitle = computed(() => {
    if (!props.videoId) return '视频加载中...'
    if (submitting.value) return '发送中...'
    if (!inputContent.value.trim()) return '请输入评论内容'
    return '发送评论'
})

// Fetch comments
const fetchComments = async () => {
    if (!props.videoId) return
    loading.value = true
    try {
        const res = await commentApi.getComments(props.videoId)
        console.log('[DesktopCommentPanel] Raw comments data:', res)
        comments.value = res || []
        
        // Log first comment to check date format
        if (comments.value.length > 0) {
            console.log('[DesktopCommentPanel] First comment createdAt:', comments.value[0].createdAt, 'Type:', typeof comments.value[0].createdAt)
        }
        
        // Calculate basic total count (top level + replies)
        let count = comments.value.length
        comments.value.forEach(c => {
            if (c.replies) count += c.replies.length
        })
        totalComments.value = count
        
    } catch (e) {
        console.error('Failed to fetch comments', e)
    } finally {
        loading.value = false
    }
}

// Reply logic (定义在 watch 之前，避免初始化错误)
const startReply = (comment: CommentVO) => {
    replyingTo.value = comment
    nextTick(() => {
        inputRef.value?.focus()
    })
}

const cancelReply = () => {
    replyingTo.value = null
}

// Toggle comment like (前端模拟，后端 API 开发中)
const toggleCommentLike = (comment: CommentVO) => {
    // TODO: 调用后端 API
    // 目前前端模拟
    comment.isLiked = !comment.isLiked
    comment.likeCount = (comment.likeCount || 0) + (comment.isLiked ? 1 : -1)
}

// Watch videoId change
watch(() => props.videoId, (newId) => {
    console.log('[DesktopCommentPanel] videoId changed to:', newId)
    if (newId) {
        comments.value = []
        fetchComments()
        inputContent.value = ''
        cancelReply()
    }
}, { immediate: true })

// Submit comment
const submitComment = async () => {
    // console.log('Submit triggered', { videoId: props.videoId, content: inputContent.value })
    if (!inputContent.value.trim() || submitting.value) return
    
    if (!props.videoId) {
        ElMessage.warning('视频信息加载中，请稍后重试')
        return
    }

    submitting.value = true
    try {
        const content = inputContent.value
        // Use reply target id as parentId if replying
        // Note: 后端 controller 接收 parentId 用于回复
        // 如果 API 不支持回复，这里 parentId 会被忽略或报错，取决于后端实现
        // 假设 CommentController: addComment(videoId, { content, parentId })
        const parentId = replyingTo.value ? replyingTo.value.id : undefined

        await commentApi.addComment(props.videoId, content, parentId)
        
        ElMessage.success('评论发布成功')
        inputContent.value = ''
        cancelReply()
        
        // Refresh comments
        await fetchComments()
        
        // Scroll to top or bottom? Maybe top for now
        if (listRef.value) listRef.value.scrollTop = 0

    } catch (e) {
        console.error('Failed to post comment', e)
        ElMessage.error('评论发布失败')
    } finally {
        submitting.value = false
    }
}

const formatNumber = (num: number) => {
    if (!num) return '0'
    if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
    return num.toString()
}

const formatTime = (dateStr: string | any) => {
    if (!dateStr) return ''
    
    try {
        const date = new Date(dateStr)
        
        // 检查日期是否有效
        if (isNaN(date.getTime())) {
            console.warn('[formatTime] Invalid date:', dateStr)
            return '时间未知'
        }
        
        const now = new Date()
        const diff = now.getTime() - date.getTime()
        
        // Less than 1 minute
        if (diff < 60000) return '刚刚'
        
        // Less than 1 hour
        if (diff < 3600000) {
            const mins = Math.floor(diff / 60000)
            return `${mins}分钟前`
        }
        
        // Less than 24 hours
        if (diff < 86400000) {
            const hours = Math.floor(diff / 3600000)
            return `${hours}小时前`
        }
        
        // Less than 7 days
        if (diff < 604800000) {
            const days = Math.floor(diff / 86400000)
            return `${days}天前`
        }
        
        return `${date.getMonth() + 1}-${date.getDate()}`
    } catch (e) {
        console.error('[formatTime] Error formatting date:', dateStr, e)
        return '时间未知'
    }
}

</script>
