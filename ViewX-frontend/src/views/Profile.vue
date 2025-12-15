<template>
  <div class="h-full w-full overflow-y-auto bg-[var(--bg)] pt-20 pb-20 px-4 sm:px-6 lg:px-8 custom-scrollbar">
    <div class="max-w-7xl mx-auto">
      
      <!-- Guest State -->
      <div v-if="!isLoggedIn" class="h-[80vh] flex flex-col items-center justify-center text-center">
         <div class="glass-panel w-full max-w-sm p-10 rounded-3xl relative overflow-hidden">
             <!-- Decor -->
             <div class="absolute top-0 right-0 -mr-10 -mt-10 w-32 h-32 bg-indigo-500/20 rounded-full blur-2xl"></div>
             <div class="absolute bottom-0 left-0 -ml-10 -mb-10 w-32 h-32 bg-purple-500/20 rounded-full blur-2xl"></div>

             <div class="relative z-10 flex flex-col items-center">
                 <div class="w-20 h-20 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center mb-6 shadow-lg shadow-indigo-500/20">
                     <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                       <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                     </svg>
                 </div>
                 <h2 class="text-2xl font-bold text-[var(--text)] mb-2">登录 ViewX</h2>
                 <p class="text-gray-400 mb-8">登录后查看个人主页、收藏视频并与其他用户互动</p>
                 <button 
                    @click="$router.push('/login')" 
                    class="w-full py-3.5 bg-white text-black font-bold rounded-xl active:scale-95 transition-transform hover:bg-gray-100"
                 >
                    立即登录
                 </button>
             </div>
         </div>
      </div>

      <!-- Loading State -->
      <div v-else-if="loading" class="flex justify-center items-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-indigo-500"></div>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="text-center py-12">
        <div class="text-red-500 mb-4">{{ error }}</div>
        <button @click="fetchProfile" class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors">
          重试
        </button>
      </div>

      <!-- Profile Content -->
      <div v-else class="space-y-8">
        
        <!-- Header Card -->
        <div class="glass-panel rounded-3xl p-8 relative overflow-hidden">
          <!-- Background Decoration -->
          <div class="absolute top-0 right-0 -mr-20 -mt-20 w-96 h-96 bg-indigo-600/20 rounded-full blur-3xl"></div>
          <div class="absolute bottom-0 left-0 -ml-20 -mb-20 w-80 h-80 bg-purple-600/20 rounded-full blur-3xl"></div>
          
          <div class="relative z-10 flex flex-col md:flex-row items-center md:items-start gap-8">
            <!-- Avatar -->
            <div class="relative group">
              <div class="w-32 h-32 rounded-full p-1 bg-gradient-to-tr from-indigo-500 via-purple-500 to-pink-500 cursor-pointer" @click="triggerAvatarUpload">
                <img 
                  :src="profile?.avatarUrl || defaultAvatar" 
                  alt="Avatar" 
                  class="w-full h-full rounded-full object-cover border-4 border-[var(--bg)]"
                />
                <!-- Upload Overlay -->
                <div class="absolute inset-0 rounded-full bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                </div>
              </div>
              <input 
                type="file" 
                ref="fileInput" 
                class="hidden" 
                accept="image/jpeg,image/png,image/gif,image/webp" 
                @change="handleAvatarUpload"
              />
            </div>
            
            <!-- Info -->
            <div class="flex-1 text-center md:text-left">
              <div class="flex flex-col md:flex-row items-center md:items-start justify-between gap-4">
                <div>
                  <h1 class="text-3xl font-bold text-[var(--text)] mb-2 flex items-center gap-2 justify-center md:justify-start">
                    {{ profile?.nickname || profile?.username }}
                    <span class="px-2 py-0.5 rounded-full bg-white/10 text-xs text-indigo-300 border border-white/5">
                        Level 21
                    </span>
                  </h1>
                  <div class="text-gray-400 max-w-lg mb-2 flex items-center justify-center md:justify-start gap-4 text-sm">
                      <span class="flex items-center gap-1"><span class="w-2 h-2 rounded-full bg-green-500"></span> Online</span>
                      <span>账号: {{ profile?.username }}</span>
                      <span v-if="profile?.address" class="flex items-center gap-1">
                          {{ profile.address }}
                      </span>
                  </div>
                  <p class="text-gray-400 max-w-lg">{{ profile?.description || '这个人很懒，什么都没有写~' }}</p>
                </div>
                
                <!-- Actions -->
                <div class="flex gap-3">
                  <button class="px-4 py-2 rounded-full bg-gray-500/10 hover:bg-gray-500/20 text-[var(--text)] transition-colors flex items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
                    </svg>
                    分享
                  </button>
                  <button @click="openEditModal" class="px-4 py-2 rounded-full bg-gray-500/10 hover:bg-gray-500/20 text-[var(--text)] transition-colors flex items-center gap-2" title="编辑资料">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                    </svg>
                    编辑资料
                  </button>
                </div>
              </div>
              
              <!-- Stats -->
              <div class="flex items-center gap-8 mt-8 border-t border-gray-500/20 pt-6 justify-center md:justify-start">
                <div class="text-center md:text-left">
                  <span class="text-xl font-bold text-[var(--text)] mr-1">{{ formatNumber(profile?.followingCount || 0) }}</span>
                  <span class="text-sm text-gray-500">关注</span>
                </div>
                <div class="text-center md:text-left">
                  <span class="text-xl font-bold text-[var(--text)] mr-1">{{ formatNumber(profile?.followersCount || 0) }}</span>
                  <span class="text-sm text-gray-500">粉丝</span>
                </div>
                <div class="text-center md:text-left">
                  <span class="text-xl font-bold text-[var(--text)] mr-1">{{ formatNumber(profile?.likeCount || 0) }}</span>
                  <span class="text-sm text-gray-500">获赞</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Content Tabs -->
        <div>
          <div class="flex items-center gap-8 border-b border-white/10 mb-6 overflow-x-auto scrollbar-hide">
            <button 
              v-for="tab in tabs" 
              :key="tab.id"
              @click="activeTab = tab.id"
              class="relative px-2 py-4 text-sm font-medium transition-colors flex items-center gap-2 whitespace-nowrap"
              :class="activeTab === tab.id ? 'text-[var(--text)]' : 'text-gray-500 hover:text-gray-300'"
            >
              <component :is="tab.icon" class="w-4 h-4" />
              {{ t(tab.labelKey) }}
              <span class="text-xs bg-white/10 px-1.5 py-0.5 rounded-full" v-if="tab.count !== undefined">{{ tab.count }}</span>
              <!-- Active Indicator -->
              <div v-if="activeTab === tab.id" class="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-500 rounded-full"></div>
            </button>
          </div>

          <!-- Tab Panels -->
          <div class="min-h-[400px]">
            <!-- Works Tab -->
            <div v-if="activeTab === 'works'">
              <div v-if="userVideos.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                 <div v-for="video in userVideos" :key="video.id" class="relative group/item">
                    <VideoCard :video="video" :show-avatar="false" @click="openVideoDetail ? openVideoDetail(video) : $emit('open-video', video)" />
                    <!-- 操作按钮 -->
                    <div class="absolute top-2 right-2 flex gap-2 opacity-0 group-hover/item:opacity-100 transition-opacity z-10">
                        <button @click.stop="editVideo(video)" class="p-2 bg-black/60 backdrop-blur-md rounded-full text-white hover:bg-indigo-600 transition-colors" title="编辑">
                            <Edit2 class="w-4 h-4" />
                        </button>
                        <button @click.stop="deleteVideo(video)" class="p-2 bg-black/60 backdrop-blur-md rounded-full text-white hover:bg-red-500 transition-colors" title="删除">
                            <Trash class="w-4 h-4" />
                        </button>
                    </div>
                 </div>
              </div>
              <EmptyState v-else title="暂无作品" desc="开始创作你的第一个视频吧！" />
            </div>

            <!-- Likes Tab -->
            <div v-else-if="activeTab === 'likes'">
               <div class="flex flex-col items-center justify-center py-20 opacity-50">
                  <Lock class="w-12 h-12 text-gray-600 mb-4" />
                  <p class="text-gray-500">喜欢列表仅自己可见</p>
               </div>
            </div>

            <!-- Collections Tab -->
            <div v-else-if="activeTab === 'collections'">
               <EmptyState title="暂无收藏" desc="你收藏的视频会出现在这里" />
            </div>

            <!-- History Tab -->
            <div v-else-if="activeTab === 'history'">
              <div v-if="historyVideos.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                 <VideoCard v-for="video in historyVideos" :key="video.id" :video="video" />
              </div>
               <EmptyState v-else title="暂无观看历史" desc="去探索更多精彩视频吧" />
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- Edit Profile Modal -->
    <div v-if="isEditing" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div class="bg-[#1a1a1a] border border-white/10 rounded-2xl w-full max-w-lg overflow-hidden shadow-2xl">
        <div class="p-6 border-b border-white/10 flex justify-between items-center">
          <h3 class="text-xl font-bold text-white">编辑个人资料</h3>
          <button @click="isEditing = false" class="text-gray-400 hover:text-white">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        
        <div class="p-6 space-y-4 max-h-[70vh] overflow-y-auto">
          <!-- Form fields -->
          <div>
            <label class="block text-sm font-medium text-gray-400 mb-1">昵称</label>
            <input v-model="editForm.nickname" type="text" class="w-full bg-white/5 border border-white/10 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-indigo-500" />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-400 mb-1">简介</label>
            <textarea v-model="editForm.description" rows="3" class="w-full bg-white/5 border border-white/10 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-indigo-500"></textarea>
          </div>
          
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-400 mb-1">性别</label>
              <select v-model="editForm.gender" class="w-full bg-white/5 border border-white/10 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-indigo-500">
                <option value="MALE">男</option>
                <option value="FEMALE">女</option>
                <option value="OTHER">其他</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-400 mb-1">年龄</label>
              <input v-model.number="editForm.age" type="number" class="w-full bg-white/5 border border-white/10 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-indigo-500" />
            </div>
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-400 mb-1">手机号</label>
            <input v-model="editForm.phone" type="text" class="w-full bg-white/5 border border-white/10 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-indigo-500" />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-400 mb-1">邮箱</label>
            <input v-model="editForm.email" type="email" class="w-full bg-white/5 border border-white/10 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-indigo-500" />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-400 mb-1">地址</label>
            <input v-model="editForm.address" type="text" class="w-full bg-white/5 border border-white/10 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-indigo-500" />
          </div>
        </div>
        
        <div class="p-6 border-t border-white/10 flex justify-end space-x-3">
          <button @click="isEditing = false" class="px-4 py-2 rounded-lg text-gray-300 hover:bg-white/5 transition-colors">取消</button>
          <button @click="saveProfile" :disabled="saving" class="px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white transition-colors flex items-center">
            <span v-if="saving" class="mr-2">
              <svg class="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            </span>
            保存
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, defineComponent, reactive } from 'vue'
import { userApi, videoApi, interactionApi, type UserProfileVO, type VideoVO } from '@/api'
import { useI18n } from 'vue-i18n'
import { Grid, Heart, Bookmark, History, Lock, Trash, Edit2 } from 'lucide-vue-next'
import VideoCard from '@/components/VideoCard.vue'

// 定义组件名称,用于 keep-alive
defineOptions({
  name: 'profile'
})

// Script
const emit = defineEmits(['open-video'])
import { inject, type Ref } from 'vue'
const openVideoDetail = inject<(v: VideoVO) => void>('openDesktopVideo')
const isLoggedIn = inject<Ref<boolean>>('isLoggedIn')

// Helper component for empty states
const EmptyState = defineComponent({
  props: ['title', 'desc'],
  template: `
    <div class="flex flex-col justify-center items-center text-center py-20">
      <div class="w-20 h-20 bg-white/5 rounded-full flex items-center justify-center mb-6">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
        </svg>
      </div>
      <h3 class="text-xl font-medium text-white mb-2">{{ title }}</h3>
      <p class="text-gray-500 max-w-xs">{{ desc }}</p>
    </div>
  `
})

const { t } = useI18n()
const loading = ref(true)
const error = ref('')
const profile = ref<UserProfileVO | null>(null)
const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'

// Tabs
const activeTab = ref('works')
const tabs = reactive([
    { id: 'works', labelKey: 'profile.works', icon: Grid, count: undefined as number | undefined }, // will be set
    { id: 'likes', labelKey: 'profile.likes', icon: Heart },
    { id: 'collections', labelKey: 'profile.collections', icon: Bookmark },
    { id: 'history', labelKey: 'profile.history', icon: History }
])

// Content Data
const userVideos = ref<VideoVO[]>([])
const historyVideos = ref<VideoVO[]>([]) // Mock history

// Edit state
const isEditing = ref(false)
const saving = ref(false)
const editForm = ref<Partial<UserProfileVO>>({})
const fileInput = ref<HTMLInputElement | null>(null)

const triggerAvatarUpload = () => {
  fileInput.value?.click()
}

const handleAvatarUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    const file = target.files[0]
    
    if (file.size > 5 * 1024 * 1024) {
      alert('图片大小不能超过 5MB')
      return
    }
    
    try {
      loading.value = true
      const newAvatarUrl = await userApi.uploadAvatar(file)
      if (profile.value) {
        profile.value.avatarUrl = newAvatarUrl
      }
      window.dispatchEvent(new Event('user-profile-updated'))
    } catch (err: any) {
      console.error('Failed to upload avatar:', err)
      alert(err.message || '头像上传失败')
    } finally {
      loading.value = false
      if (fileInput.value) {
        fileInput.value.value = ''
      }
    }
  }
}

const fetchProfile = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await userApi.getMyProfile()
    profile.value = res
    // Update tab counts if available
    tabs[0].count = profile.value?.videoCount

    // 获取关注统计数据
    if (profile.value?.userId) {
      try {
        const stats = await interactionApi.getFollowStats(profile.value.userId)
        // 将统计数据更新到 profile 中
        profile.value.followingCount = stats.followingCount
        profile.value.followersCount = stats.followerCount
      } catch (err) {
        console.error('Failed to fetch follow stats:', err)
      }
    }

  } catch (err) {
    console.error('Failed to fetch profile:', err)
    error.value = '获取个人信息失败,请稍后重试'
  } finally {
    loading.value = false
  }
}

const fetchContent = async () => {
    // Fetch user videos (works)
    try {
        // 使用真实接口获取我的视频
        const res = await videoApi.getMyVideos()
        userVideos.value = res
        
        // Mock history (历史记录接口暂未实现，使用推荐流模拟)
        const feed = await videoApi.getFeed()
        historyVideos.value = feed.slice(0, 4)
    } catch(e) {
        console.error("Failed to fetch content", e)
    }
}

const openEditModal = () => {
  if (profile.value) {
    editForm.value = { ...profile.value }
    isEditing.value = true
  }
}

const saveProfile = async () => {
  if (!editForm.value) return
  
  saving.value = true
  try {
    await userApi.updateProfile(editForm.value)
    await fetchProfile()
    isEditing.value = false
    window.dispatchEvent(new Event('user-profile-updated'))
  } catch (err) {
    console.error('Failed to update profile:', err)
    alert('更新失败，请重试')
  } finally {
    saving.value = false
  }
}

const formatNumber = (num: number) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num
}

const deleteVideo = async (video: VideoVO) => {
  if (!confirm('确定要删除这个视频吗？此操作不可恢复。')) return
  try {
    await videoApi.deleteVideo(video.id)
    userVideos.value = userVideos.value.filter(v => v.id !== video.id)
    // alert('删除成功') // 避免过多弹窗，操作成功即可
  } catch (e) {
    console.error('Delete failed:', e)
    alert('删除失败')
  }
}

const editVideo = async (video: VideoVO) => {
  // 简单的编辑实现 MVP
  const newTitle = prompt('修改标题:', video.title)
  if (newTitle === null || newTitle === video.title) return
  
  const newDesc = prompt('修改描述:', video.description || '')
  if (newDesc === null) return

  try {
    await videoApi.updateVideo(video.id, {
      title: newTitle,
      description: newDesc,
      duration: video.duration // 保持原样
    })
    
    // 更新本地数据
    const index = userVideos.value.findIndex(v => v.id === video.id)
    if (index !== -1) {
      userVideos.value[index].title = newTitle
      userVideos.value[index].description = newDesc
    }
    alert('更新成功')
  } catch (e) {
    console.error('Update failed:', e)
    alert('更新失败')
  }
}

onMounted(() => {
  if (isLoggedIn?.value) {
    fetchProfile()
    fetchContent()
  } else {
    loading.value = false
  }
})
</script>

<style scoped>
.glass-panel {
  background: var(--surface);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border);
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.1);
}

.scrollbar-hide::-webkit-scrollbar {
    display: none;
}
.scrollbar-hide {
    -ms-overflow-style: none;
    scrollbar-width: none;
}


</style>