<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-800 dark:text-white">用户管理</h1>
      <div class="flex gap-3">
        <input 
          v-model="searchKeyword"
          type="text" 
          placeholder="搜索用户..." 
          class="px-4 py-2 rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-800 dark:text-white focus:ring-2 focus:ring-indigo-500 outline-none"
        >
        <button @click="searchUsers" class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors">
          搜索
        </button>
        <button @click="openCreateModal" class="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors flex items-center gap-2">
          <el-icon><Plus /></el-icon> 添加用户
        </button>
      </div>
    </div>

    <!-- User List Table -->
    <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 overflow-x-auto">
      <div v-if="loading" class="p-8 text-center text-gray-500">加载中...</div>
      <table v-else class="w-full text-left min-w-[800px]">
        <thead class="bg-gray-50 dark:bg-gray-700/50 text-gray-500 dark:text-gray-400 text-sm">
          <tr>
            <th class="px-4 md:px-6 py-4 font-medium">用户</th>
            <th class="px-4 md:px-6 py-4 font-medium">角色</th>
            <th class="px-4 md:px-6 py-4 font-medium">注册时间</th>
            <th class="px-4 md:px-6 py-4 font-medium">状态</th>
            <th class="px-4 md:px-6 py-4 font-medium">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
          <tr v-for="user in users" :key="user.id" class="hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-colors">
            <td class="px-4 md:px-6 py-4">
              <div class="flex items-center gap-3">
                <div class="w-8 h-8 rounded-full bg-indigo-100 dark:bg-indigo-900/50 flex items-center justify-center text-indigo-600 dark:text-indigo-400 font-bold text-xs overflow-hidden flex-shrink-0">
                  <img v-if="user.details?.avatar" :src="user.details.avatar" class="w-full h-full object-cover" />
                  <span v-else>{{ user.username.charAt(0).toUpperCase() }}</span>
                </div>
                <div>
                  <div class="font-medium text-gray-800 dark:text-white">
                    {{ user.username }}
                    <span v-if="user.nickname && user.nickname !== user.username" class="text-gray-500 dark:text-gray-400 font-normal">({{ user.nickname }})</span>
                  </div>
                  <div class="text-xs text-gray-500">{{ user.email }}</div>
                </div>
              </div>
            </td>
            <td class="px-4 md:px-6 py-4">
              <span class="text-sm text-gray-600 dark:text-gray-300">{{ formatRole(user.role) }}</span>
            </td>
            <td class="px-4 md:px-6 py-4">
              <span class="text-sm text-gray-600 dark:text-gray-300">{{ formatDate(user.registerTime) }}</span>
            </td>
            <td class="px-4 md:px-6 py-4">
              <div class="flex items-center gap-2">
                <span :class="[
                  'px-2 py-1 text-xs font-medium rounded-full whitespace-nowrap',
                  getUserStatusClass(user)
                ]">
                  {{ getUserStatusLabel(user) }}
                </span>
                <button 
                  v-if="!user.enabled"
                  @click="toggleUserStatus(user, true)"
                  class="text-xs text-green-600 dark:text-green-400 hover:underline whitespace-nowrap"
                  title="启用账户"
                >
                  启用
                </button>
              </div>
            </td>
            <td class="px-4 md:px-6 py-4">
              <div class="flex gap-3 whitespace-nowrap">
                <button @click="openUploadModal(user)" class="text-sm text-indigo-600 dark:text-indigo-400 hover:underline flex items-center gap-1">
                  <el-icon><VideoCamera /></el-icon> 发布视频
                </button>
                <button @click="openEditModal(user)" class="text-sm text-gray-600 dark:text-gray-400 hover:underline flex items-center gap-1">
                  <el-icon><Edit /></el-icon> 编辑
                </button>
                <button @click="confirmDeleteUser(user)" class="text-sm text-red-600 dark:text-red-400 hover:underline flex items-center gap-1">
                  <el-icon><Delete /></el-icon> 删除
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Create User Modal -->
    <div v-if="showCreateModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div class="bg-white dark:bg-gray-800 rounded-2xl w-full max-w-lg overflow-hidden shadow-2xl">
        <div class="p-6 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center">
          <h3 class="text-xl font-bold text-gray-800 dark:text-white">添加新用户</h3>
          <button @click="closeCreateModal" class="text-gray-400 hover:text-gray-600 dark:hover:text-white">
            <el-icon><Close /></el-icon>
          </button>
        </div>
        
        <div class="p-6 space-y-4 max-h-[70vh] overflow-y-auto">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">用户名 *</label>
            <input v-model="createForm.username" type="text" class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="输入用户名" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">密码 *</label>
            <input v-model="createForm.password" type="password" class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="输入密码（默认123456）" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">邮箱 *</label>
            <input v-model="createForm.email" type="email" class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="输入邮箱" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">昵称</label>
            <input v-model="createForm.nickname" type="text" class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="输入昵称（可选）" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">手机号</label>
            <input v-model="createForm.phone" type="text" class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="输入手机号（可选）" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">角色</label>
            <select v-model="createForm.role" class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 outline-none">
              <option value="USER">普通用户</option>
              <option value="ADMIN">管理员</option>
              <option value="SUPER_ADMIN">超级管理员</option>
              <option value="REVIEWER">审核员</option>
            </select>
          </div>
        </div>
        
        <div class="p-6 border-t border-gray-200 dark:border-gray-700 flex justify-end space-x-3">
          <button @click="closeCreateModal" class="px-4 py-2 rounded-lg text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">取消</button>
          <button @click="submitCreate" :disabled="creating" class="px-4 py-2 rounded-lg bg-green-600 hover:bg-green-700 text-white transition-colors flex items-center disabled:opacity-50 disabled:cursor-not-allowed">
            <span v-if="creating" class="mr-2 animate-spin">⟳</span>
            {{ creating ? '创建中...' : '创建用户' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Upload Video Modal -->
    <div v-if="showUploadModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div class="bg-white dark:bg-gray-800 rounded-2xl w-full max-w-lg overflow-hidden shadow-2xl">
        <div class="p-6 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center">
          <h3 class="text-xl font-bold text-gray-800 dark:text-white">
            为 {{ selectedUser?.nickname || selectedUser?.username }} 发布视频
          </h3>
          <button @click="closeUploadModal" class="text-gray-400 hover:text-gray-600 dark:hover:text-white">
            <el-icon><Close /></el-icon>
          </button>
        </div>
        
        <div class="p-6 space-y-4 max-h-[70vh] overflow-y-auto">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">标题</label>
            <input v-model="uploadForm.title" type="text" class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="输入视频标题" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">描述</label>
            <textarea v-model="uploadForm.description" rows="3" class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="输入视频描述"></textarea>
          </div>

          <!-- Video File Upload -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">视频文件</label>
            <div class="flex items-center gap-4">
              <input type="file" ref="videoInputRef" accept="video/*" @change="handleVideoFileChange" class="hidden" />
              <button @click="() => videoInputRef?.click()" class="px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors">
                选择视频
              </button>
              <span v-if="videoFile" class="text-sm text-green-600 dark:text-green-400 truncate max-w-[200px]">{{ videoFile.name }}</span>
            </div>
          </div>

          <!-- Cover Image Upload -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">封面图片</label>
            <div class="flex items-center gap-4">
              <input type="file" ref="coverInputRef" accept="image/*" @change="handleCoverFileChange" class="hidden" />
              <button @click="() => coverInputRef?.click()" class="px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors">
                选择封面
              </button>
              <span v-if="coverFile" class="text-sm text-green-600 dark:text-green-400 truncate max-w-[200px]">{{ coverFile.name }}</span>
            </div>
          </div>
        </div>
        
        <div class="p-6 border-t border-gray-200 dark:border-gray-700 flex justify-end space-x-3">
          <button @click="closeUploadModal" class="px-4 py-2 rounded-lg text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">取消</button>
          <button @click="submitUpload" :disabled="uploading" class="px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white transition-colors flex items-center disabled:opacity-50 disabled:cursor-not-allowed">
            <span v-if="uploading" class="mr-2 animate-spin">⟳</span>
            {{ uploading ? '发布中...' : '发布视频' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Edit User Modal -->
    <div v-if="showEditModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div class="bg-white dark:bg-gray-800 rounded-2xl w-full max-w-lg overflow-hidden shadow-2xl">
        <div class="p-6 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center">
          <h3 class="text-xl font-bold text-gray-800 dark:text-white">
            编辑用户: {{ selectedUser?.username }}
          </h3>
          <button @click="closeEditModal" class="text-gray-400 hover:text-gray-600 dark:hover:text-white">
            <el-icon><Close /></el-icon>
          </button>
        </div>
        
        <div class="p-6 space-y-4 max-h-[70vh] overflow-y-auto">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">昵称</label>
            <input v-model="editForm.nickname" type="text" class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 outline-none" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">邮箱</label>
            <input v-model="editForm.email" type="email" class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 outline-none" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">手机号</label>
            <input v-model="editForm.phone" type="text" class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 outline-none" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">角色</label>
            <select v-model="editForm.role" class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 outline-none">
              <option value="USER">普通用户</option>
              <option value="ADMIN">管理员</option>
              <option value="SUPER_ADMIN">超级管理员</option>
              <option value="REVIEWER">审核员</option>
            </select>
          </div>

          <!-- 账户状态管理 -->
          <div class="border-t border-gray-200 dark:border-gray-700 pt-4 mt-4">
            <h4 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">账户状态</h4>
            
            <div>
              <select v-model="editForm.status" class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 outline-none">
                <option value="ACTIVE">正常 (Active)</option>
                <option value="DISABLED">禁用 (Disabled)</option>
                <option value="LOCKED">锁定 (Locked)</option>
                <option value="EXPIRED">过期 (Expired)</option>
              </select>
              <p class="mt-2 text-xs text-gray-500 dark:text-gray-400">
                <span v-if="editForm.status === 'ACTIVE'">账户可以正常登录和使用。</span>
                <span v-else-if="editForm.status === 'DISABLED'">账户被管理员手动禁用，无法登录。</span>
                <span v-else-if="editForm.status === 'LOCKED'">账户因多次登录失败或管理员操作被锁定。</span>
                <span v-else-if="editForm.status === 'EXPIRED'">账户有效期已过。</span>
              </p>
            </div>
          </div>
        </div>
        
        <div class="p-6 border-t border-gray-200 dark:border-gray-700 flex justify-end space-x-3">
          <button @click="closeEditModal" class="px-4 py-2 rounded-lg text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">取消</button>
          <button @click="submitEdit" :disabled="saving" class="px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white transition-colors flex items-center disabled:opacity-50 disabled:cursor-not-allowed">
            <span v-if="saving" class="mr-2 animate-spin">⟳</span>
            {{ saving ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi, videoApi } from '@/api'
import { VideoCamera, Close, Edit, Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref<any[]>([])
const loading = ref(false)
const searchKeyword = ref('')

// Create Modal State
const showCreateModal = ref(false)
const creating = ref(false)
const createForm = ref({
  username: '',
  password: '',
  email: '',
  phone: '',
  nickname: '',
  role: 'USER'
})

// Upload Modal State
const showUploadModal = ref(false)
const selectedUser = ref<any>(null)
const uploading = ref(false)
const uploadForm = ref({
  title: '',
  description: '',
  videoUrl: '',
  thumbnailUrl: ''
})
const videoFile = ref<File | null>(null)
const coverFile = ref<File | null>(null)
const videoInputRef = ref<HTMLInputElement | null>(null)
const coverInputRef = ref<HTMLInputElement | null>(null)

// Edit Modal State
const showEditModal = ref(false)
const saving = ref(false)
const editForm = ref({
  nickname: '',
  email: '',
  phone: '',
  role: 'USER',
  status: 'ACTIVE'
})

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await adminApi.getUsers()
    // Handle pagination wrapper if exists
    if (res.records) {
      users.value = res.records
    } else if (Array.isArray(res)) {
      users.value = res
    } else if (res.data && Array.isArray(res.data)) {
      users.value = res.data
    } else {
      users.value = []
    }
  } catch (e) {
    console.error('Failed to fetch users:', e)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const formatRole = (roleObj: any) => {
  if (!roleObj) return '普通用户'
  return roleObj.label || roleObj
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString()
}

// 获取用户状态标签
const getUserStatusLabel = (user: any) => {
  if (!user.enabled) return '账户禁用'
  if (!user.accountNonLocked) return '账户已锁定'
  if (!user.accountNonExpired) return '已过期'
  if (!user.credentialsNonExpired) return '凭证过期'
  return '正常'
}

// 获取用户状态样式类
const getUserStatusClass = (user: any) => {
  if (!user.enabled) return 'text-red-600 bg-red-50 dark:bg-red-900/30 dark:text-red-400'
  if (!user.accountNonLocked) return 'text-orange-600 bg-orange-50 dark:bg-orange-900/30 dark:text-orange-400'
  if (!user.accountNonExpired || !user.credentialsNonExpired) return 'text-orange-600 bg-orange-50 dark:bg-orange-900/30 dark:text-orange-400'
  return 'text-green-600 bg-green-50 dark:bg-green-900/30 dark:text-green-400'
}

// 快速切换用户启用/禁用状态
const toggleUserStatus = async (user: any, enabled: boolean) => {
  try {
    const status = enabled ? 'ACTIVE' : 'DISABLED'
    await adminApi.updateUserStatus(user.id, status)
    ElMessage.success(enabled ? '账户已启用' : '账户已禁用')
    fetchUsers()
  } catch (e) {
    console.error('Toggle status failed:', e)
    ElMessage.error('状态更新失败')
  }
}

// Upload Modal Actions
const openUploadModal = (user: any) => {
  selectedUser.value = user
  uploadForm.value = { title: '', description: '', videoUrl: '', thumbnailUrl: '' }
  videoFile.value = null
  coverFile.value = null
  showUploadModal.value = true
}

const closeUploadModal = () => {
  showUploadModal.value = false
  selectedUser.value = null
}

const handleVideoFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    videoFile.value = target.files[0]
  }
}

const handleCoverFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    coverFile.value = target.files[0]
  }
}

const submitUpload = async () => {
  if (!uploadForm.value.title) {
    ElMessage.warning('请输入视频标题')
    return
  }
  if (!videoFile.value) {
    ElMessage.warning('请选择视频文件')
    return
  }
  if (!coverFile.value) {
    ElMessage.warning('请选择封面图片')
    return
  }

  uploading.value = true
  try {
    // Note: This upload functionality needs proper implementation
    // For now, we'll use the standard video upload API
    if (!videoFile.value || !coverFile.value) return
    
    // Upload video with metadata
    const videoId = await videoApi.uploadVideo(videoFile.value, coverFile.value, {
      title: uploadForm.value.title,
      description: uploadForm.value.description || '',
      duration: 0, // This should be calculated from video file
      visibility: 'PUBLIC'
    })

    console.log('Video uploaded successfully, ID:', videoId)

    ElMessage.success('视频发布成功')
    closeUploadModal()
  } catch (e) {
    console.error('Upload failed:', e)
    ElMessage.error('发布失败，请重试')
  } finally {
    uploading.value = false
  }
}

// Determine status enum from user object
const determineUserStatus = (user: any) => {
  if (!user.enabled) return 'DISABLED'
  if (!user.accountNonLocked) return 'LOCKED'
  if (!user.accountNonExpired) return 'EXPIRED'
  // credentialsNonExpired usually doesn't block login immediately or is handled differently, 
  // but for admin setting status, we usually care about the first 3.
  return 'ACTIVE'
}

// Edit Modal Actions
const openEditModal = (user: any) => {
  selectedUser.value = user
  
  let roleCode = 'USER'
  const roleStr = user.role?.label || user.role
  if (roleStr === '超级管理员' || roleStr === 'SUPER_ADMIN') roleCode = 'SUPER_ADMIN'
  else if (roleStr === '管理员' || roleStr === 'ADMIN') roleCode = 'ADMIN'
  else if (roleStr === '审核员' || roleStr === 'REVIEWER') roleCode = 'REVIEWER'
  
  editForm.value = {
    nickname: user.nickname || '',
    email: user.email || '',
    phone: user.phone || '',
    role: roleCode,
    status: determineUserStatus(user)
  }
  showEditModal.value = true
}

const closeEditModal = () => {
  showEditModal.value = false
  selectedUser.value = null
}

const submitEdit = async () => {
  if (!selectedUser.value) return
  
  saving.value = true
  try {
    // 1. Update Profile Info
    await adminApi.updateUser(selectedUser.value.id, {
      nickname: editForm.value.nickname,
      email: editForm.value.email,
      phone: editForm.value.phone,
      role: editForm.value.role
    })

    // 2. Update Status
    await adminApi.updateUserStatus(selectedUser.value.id, editForm.value.status)

    ElMessage.success('用户更新成功')
    closeEditModal()
    fetchUsers() // Refresh list
  } catch (e) {
    console.error('Update failed:', e)
    ElMessage.error('更新失败，请重试')
  } finally {
    saving.value = false
  }
}

// Create User Actions
const openCreateModal = () => {
  createForm.value = {
    username: '',
    password: '',
    email: '',
    phone: '',
    nickname: '',
    role: 'USER'
  }
  showCreateModal.value = true
}

const closeCreateModal = () => {
  showCreateModal.value = false
}

const submitCreate = async () => {
  if (!createForm.value.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!createForm.value.email) {
    ElMessage.warning('请输入邮箱')
    return
  }
  
  creating.value = true
  try {
    await adminApi.createUser({
      username: createForm.value.username,
      password: createForm.value.password || '123456',
      email: createForm.value.email,
      phone: createForm.value.phone,
      nickname: createForm.value.nickname,
      role: createForm.value.role
    })
    
    ElMessage.success('用户创建成功')
    closeCreateModal()
    fetchUsers()
  } catch (e: any) {
    console.error('Create user failed:', e)
    ElMessage.error(e.message || '创建失败，请重试')
  } finally {
    creating.value = false
  }
}

// Delete User
const confirmDeleteUser = async (user: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${user.nickname || user.username}" 吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    await adminApi.deleteUser(user.id)
    ElMessage.success('用户已删除')
    fetchUsers()
  } catch (e: any) {
    if (e !== 'cancel') {
      console.error('Delete user failed:', e)
      ElMessage.error('删除失败，请重试')
    }
  }
}

// Search Users
const searchUsers = () => {
  // TODO: Implement search functionality
  ElMessage.info('搜索功能开发中...')
}

onMounted(() => {
  fetchUsers()
})
</script>
