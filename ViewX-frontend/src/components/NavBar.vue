<template>
  <header class="glass-panel h-16 flex items-center justify-between px-6 z-40 fixed top-0 w-full border-b-0">
    <!-- Logo -->
    <div class="flex items-center gap-3 group cursor-pointer" @click="$emit('navigate', 'home')">
      <div
        class="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 flex items-center justify-center shadow-lg group-hover:rotate-12 spring-transition">
        <Play class="w-4 h-4 text-white fill-white" />
      </div>
      <span
        :class="['text-xl font-bold tracking-tight bg-clip-text text-transparent bg-gradient-to-r', theme === 'light' ? 'from-indigo-600 to-purple-600' : 'from-white to-gray-400']">ViewX</span>
    </div>

    <!-- 搜索栏 -->
    <div class="hidden md:flex flex-1 max-w-xl mx-8 relative">
      <input type="text" placeholder="搜索视频、UP主..."
        class="w-full bg-[var(--bg)] border border-[var(--border)] rounded-xl py-2.5 pl-10 pr-4 text-sm focus:outline-none focus:border-[var(--primary)] transition-all placeholder-[var(--muted)] text-[var(--text)]">
      <Search class="absolute left-3.5 top-2.5 w-4 h-4 text-[var(--muted)]" />
    </div>

    <!-- 用户区 -->
    <div class="flex items-center gap-5">
      <button class="relative hover:text-[var(--text)] text-[var(--muted)] transition-colors click-spring">
        <Bell class="w-5 h-5" />
        <span class="absolute top-0 right-0 w-2 h-2 bg-red-500 rounded-full border border-[var(--bg)]"></span>
      </button>
      
      <div v-if="isLoggedIn" class="flex items-center gap-3">
       <!-- User Profile Dropdown -->
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="flex items-center gap-2 cursor-pointer outline-none">
          <button class="w-10 h-10 rounded-full overflow-hidden border-2 border-transparent hover:border-indigo-500 transition-all duration-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2">
            <img 
              :src="userProfile?.avatarUrl || defaultAvatar" 
              alt="User" 
              class="w-full h-full object-cover"
            />
          </button>
          <div class="hidden md:flex flex-col items-start ml-1">
            <span class="text-sm font-medium text-[var(--text)] leading-tight">
              {{ userProfile?.nickname || userProfile?.username || 'User' }}
            </span>
            <span v-if="userProfile?.nickname && userProfile?.username" class="text-xs text-[var(--muted)] leading-tight">
              @{{ userProfile?.username }}
            </span>
          </div>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>个人中心
            </el-dropdown-item>
            
            <!-- Admin Panel Link -->
            <el-dropdown-item 
              v-if="isAdminOrModerator" 
              command="admin"
              divided
            >
              <el-icon><Management /></el-icon>管理后台
            </el-dropdown-item>

            <el-dropdown-item command="settings">
              <el-icon><Setting /></el-icon>设置
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      </div>
      <button v-else @click="$emit('open-login')" class="text-sm font-bold text-white bg-indigo-600 px-4 py-1.5 rounded-full hover:bg-indigo-500 transition-colors">
        登录
      </button>

      <!-- Mobile Menu Button -->
      <button class="md:hidden text-[var(--muted)] hover:text-[var(--text)]" @click="$emit('toggle-sidebar')">
        <Menu class="w-6 h-6" />
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { Play } from 'lucide-vue-next'
import { Search, Bell, Menu, User, Setting, SwitchButton, Management } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { userApi, type UserProfileVO } from '@/api'

const props = defineProps<{
  theme: 'light' | 'dark'
  isLoggedIn: boolean
}>()

const emit = defineEmits<{
  'toggle-theme': []
  'toggle-sidebar': []
  'open-login': []
  'logout': []
  'navigate': [view: 'home' | 'settings' | 'profile' | 'admin']
}>()

const userProfile = ref<UserProfileVO | null>(null)
const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'

const isAdminOrModerator = computed(() => {
  if (!userProfile.value || !userProfile.value.role) return false
  const role = userProfile.value.role
  // 匹配后端 Role.java 中的 label: "超级管理员", "管理员", "审核员"
  return role === '超级管理员' || 
         role === '管理员' || 
         role === '审核员' ||
         // 保留英文/代码回退以防万一
         role.includes('Admin') || 
         role.includes('Moderator') ||
         role === 'ROLE_SUPER_ADMIN' || 
         role === 'ROLE_ADMIN' || 
         role === 'ROLE_REVIEWER'
})

const fetchUserProfile = async () => {
  if (props.isLoggedIn) {
    try {
      const res = await userApi.getMyProfile()
      userProfile.value = res as any
    } catch (error) {
      console.error('Failed to fetch user profile:', error)
    }
  } else {
    userProfile.value = null
  }
}

watch(() => props.isLoggedIn, (newVal) => {
  if (newVal) {
    fetchUserProfile()
  } else {
    userProfile.value = null
  }
})

onMounted(() => {
  fetchUserProfile()
  window.addEventListener('user-profile-updated', fetchUserProfile)
})

onUnmounted(() => {
  window.removeEventListener('user-profile-updated', fetchUserProfile)
})

const handleCommand = (command: string) => {
  console.log('Dropdown command:', command)
  if (command === 'logout') {
    ElMessageBox.confirm(
      '确定要退出登录吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
      .then(() => {
        console.log('Confirmed logout')
        emit('logout')
      })
      .catch((e) => {
        console.log('Logout cancelled', e)
      })
  } else if (command === 'profile') {
    emit('navigate', 'profile')
  } else if (command === 'settings') {
    emit('navigate', 'settings')
  } else if (command === 'admin') {
    emit('navigate', 'admin')
  }
}
</script>