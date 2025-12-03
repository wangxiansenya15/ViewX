<template>
  <div class="min-h-screen bg-[#0f0f0f] pt-20 pb-10 px-4 sm:px-6 lg:px-8">
    <div class="max-w-5xl mx-auto">
      
      <!-- Loading State -->
      <div v-if="loading" class="flex justify-center items-center h-64">
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
      <div v-else class="space-y-6">
        
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
                  class="w-full h-full rounded-full object-cover border-4 border-[#0f0f0f]"
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
                  <h1 class="text-3xl font-bold text-white mb-2">{{ profile?.nickname || profile?.username }}</h1>
                  <p class="text-gray-400 max-w-lg">{{ profile?.description || '这个人很懒，什么都没有写~' }}</p>
                </div>
                
                <!-- Actions -->
                <div class="flex gap-3">
                  <button class="px-4 py-2 rounded-full bg-white/10 hover:bg-white/20 text-white transition-colors flex items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
                    </svg>
                    分享
                  </button>
                  <button @click="openEditModal" class="px-4 py-2 rounded-full bg-white/10 hover:bg-white/20 text-white transition-colors flex items-center gap-2" title="编辑资料">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                    </svg>
                    编辑资料
                  </button>
                </div>
              </div>
              
              <!-- Stats -->
              <div class="grid grid-cols-4 gap-4 mt-10 border-t border-white/5 pt-6">
                <div class="text-center">
                  <div class="text-2xl font-bold text-white">{{ formatNumber(profile?.followersCount || 0) }}</div>
                  <div class="text-xs text-gray-500 uppercase tracking-wider mt-1">粉丝</div>
                </div>
                <div class="text-center border-l border-white/5">
                  <div class="text-2xl font-bold text-white">{{ formatNumber(profile?.followingCount || 0) }}</div>
                  <div class="text-xs text-gray-500 uppercase tracking-wider mt-1">关注</div>
                </div>
                <div class="text-center border-l border-white/5">
                  <div class="text-2xl font-bold text-white">{{ formatNumber(profile?.videoCount || 0) }}</div>
                  <div class="text-xs text-gray-500 uppercase tracking-wider mt-1">视频</div>
                </div>
                <div class="text-center border-l border-white/5">
                  <div class="text-2xl font-bold text-white">{{ formatNumber(profile?.likeCount || 0) }}</div>
                  <div class="text-xs text-gray-500 uppercase tracking-wider mt-1">获赞</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Details Grid -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
          
          <!-- Left Column: Personal Info -->
          <div class="md:col-span-1 space-y-6">
            <div class="glass-panel p-6 rounded-2xl">
              <h3 class="text-lg font-semibold text-white mb-4 flex items-center">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                个人信息
              </h3>
              <div class="space-y-4 text-sm">
                <div class="flex justify-between">
                  <span class="text-gray-500">用户名</span>
                  <span class="text-gray-300 font-mono">@{{ profile?.username }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-gray-500">角色</span>
                  <span class="px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 text-xs">{{ profile?.role || 'User' }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-gray-500">性别</span>
                  <span class="text-gray-300">{{ formatGender(profile?.gender) }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-gray-500">年龄</span>
                  <span class="text-gray-300">{{ profile?.age || '-' }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-gray-500">注册时间</span>
                  <span class="text-gray-300">{{ formatDate(profile?.createdAt) }}</span>
                </div>
              </div>
            </div>
            
            <div class="glass-panel p-6 rounded-2xl">
              <h3 class="text-lg font-semibold text-white mb-4 flex items-center">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
                联系方式
              </h3>
              <div class="space-y-4 text-sm">
                <div class="group">
                  <div class="text-gray-500 mb-1">邮箱</div>
                  <div class="text-gray-300 truncate">{{ profile?.email || '未绑定' }}</div>
                </div>
                <div class="group">
                  <div class="text-gray-500 mb-1">手机</div>
                  <div class="text-gray-300">{{ profile?.phone || '未绑定' }}</div>
                </div>
                <div class="group">
                  <div class="text-gray-500 mb-1">地址</div>
                  <div class="text-gray-300">{{ profile?.address || '未设置' }}</div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- Right Column: Recent Activity (Placeholder) -->
          <div class="md:col-span-2 space-y-6">
             <div class="glass-panel p-6 rounded-2xl min-h-[400px] flex flex-col justify-center items-center text-center">
                <div class="w-16 h-16 bg-white/5 rounded-full flex items-center justify-center mb-4">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
                  </svg>
                </div>
                <h3 class="text-xl font-medium text-white mb-2">暂无视频作品</h3>
                <p class="text-gray-500 max-w-xs">该用户还没有发布任何视频内容。期待Ta的精彩创作！</p>
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
import { ref, onMounted } from 'vue'
import { userApi, type UserProfileVO } from '@/api'

const loading = ref(true)
const error = ref('')
const profile = ref<UserProfileVO | null>(null)
const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'

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
    
    // 验证文件大小 (5MB)
    if (file.size > 5 * 1024 * 1024) {
      alert('图片大小不能超过 5MB')
      return
    }
    
    try {
      // 乐观更新：先显示 loading 或预览（这里简单处理，直接上传）
      loading.value = true
      const newAvatarUrl = await userApi.uploadAvatar(file)
      
      // 更新本地状态
      if (profile.value) {
        profile.value.avatarUrl = newAvatarUrl
      }
      
      // 提示成功
      // alert('头像上传成功')
    } catch (err: any) {
      console.error('Failed to upload avatar:', err)
      alert(err.message || '头像上传失败')
    } finally {
      loading.value = false
      // 清空 input，允许重复上传同一文件
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
    // 假设后端返回的是 Result<UserProfileVO>，但 axios 拦截器可能已经解包了 data
    // 如果没有解包，可能需要 res.data
    profile.value = res as any 
  } catch (err) {
    console.error('Failed to fetch profile:', err)
    error.value = '获取个人信息失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const openEditModal = () => {
  if (profile.value) {
    // Copy profile data to edit form
    editForm.value = { ...profile.value }
    isEditing.value = true
  }
}

const saveProfile = async () => {
  if (!editForm.value) return
  
  saving.value = true
  try {
    await userApi.updateProfile(editForm.value)
    // Refresh profile
    await fetchProfile()
    isEditing.value = false
    // Show success message (optional, maybe alert for now)
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

const formatGender = (gender?: string) => {
  const map: Record<string, string> = {
    'MALE': '男',
    'FEMALE': '女',
    'OTHER': '其他'
  }
  return gender ? (map[gender] || '未知') : '保密'
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString()
}

onMounted(() => {
  fetchProfile()
})
</script>

<style scoped>
.glass-panel {
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.05);
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.2);
}

/* Light mode overrides */
:root[data-theme='light'] .glass-panel {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.05);
}

:root[data-theme='light'] .text-white {
  color: #1a1a1a;
}

:root[data-theme='light'] .text-gray-300 {
  color: #4a4a4a;
}

:root[data-theme='light'] .text-gray-400 {
  color: #6a6a6a;
}

:root[data-theme='light'] .text-gray-500 {
  color: #8a8a8a;
}
</style>