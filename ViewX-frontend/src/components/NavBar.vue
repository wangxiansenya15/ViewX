<template>
  <header class="glass-panel h-14 md:h-16 flex items-center justify-between px-3 md:px-6 z-40 fixed top-0 w-full border-b-0 transition-all duration-300">
    
    <!-- Left Section: Menu & Logo -->
    <div class="flex items-center gap-2 md:gap-3">
      <!-- Mobile Menu Button -->
      <button class="md:hidden p-1 text-[var(--muted)] hover:text-[var(--text)] active:scale-90 transition-transform" 
        @click="$emit('toggle-sidebar')">
        <Menu class="w-6 h-6" />
      </button>

      <!-- Logo -->
      <div class="flex items-center gap-2 md:gap-3 group cursor-pointer select-none" @click="$emit('navigate', 'home')">
        <div
          class="w-8 h-8 md:w-9 md:h-9 rounded-xl bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 flex items-center justify-center shadow-lg group-hover:rotate-12 spring-transition">
          <Play class="w-3.5 h-3.5 md:w-4 md:h-4 text-white fill-white" />
        </div>
        <span
          :class="['text-lg md:text-xl font-bold tracking-tight bg-clip-text text-transparent bg-gradient-to-r', theme === 'light' ? 'from-indigo-600 to-purple-600' : 'from-white to-gray-400']">ViewX</span>
      </div>
    </div>

    <!-- Center Section: Search Bar (Desktop Only) -->
    <div class="hidden md:flex flex-1 max-w-xl mx-8 relative group">
      <input type="text" placeholder="搜索视频、UP主..."
        class="w-full bg-[var(--bg)] border border-[var(--border)] rounded-xl py-2 pl-10 pr-4 text-sm focus:outline-none focus:border-[var(--primary)] transition-all placeholder-[var(--muted)] text-[var(--text)] focus:ring-2 focus:ring-[var(--primary)]/20 shadow-sm">
      <Search class="absolute left-3.5 top-2.5 w-4 h-4 text-[var(--muted)] group-focus-within:text-[var(--primary)] transition-colors" />
    </div>

    <!-- Right Section: User Actions -->
    <div class="flex items-center gap-2 md:gap-4">
      <!-- Mobile Search Button -->
      <button class="md:hidden p-2 text-[var(--muted)] hover:text-[var(--text)] active:scale-95 transition-transform">
        <Search class="w-5 h-5" />
      </button>

      <!-- Upload Button (Desktop) -->
      <button 
        v-if="isLoggedIn"
        @click="$emit('navigate', 'upload')"
        class="hidden md:flex items-center gap-2 px-3 py-1.5 rounded-full bg-white/5 hover:bg-white/10 text-[var(--text)] transition-all border border-transparent hover:border-indigo-500/30 group active:scale-95"
      >
        <Upload class="w-4 h-4 text-indigo-500 group-hover:scale-110 transition-transform" />
        <span class="text-sm font-medium">投稿</span>
      </button>

      <!-- Notification Bell -->
      <NotificationBell v-if="isLoggedIn" />
      
      <!-- User Profile -->
      <div v-if="isLoggedIn" class="flex items-center">
        <el-dropdown trigger="click" @command="handleCommand" popper-class="custom-dropdown">
          <div class="flex items-center gap-2 cursor-pointer outline-none">
            <button class="w-8 h-8 md:w-10 md:h-10 rounded-full overflow-hidden border-2 border-transparent hover:border-indigo-500 transition-all duration-300 ring-2 ring-transparent focus:ring-indigo-500/50">
              <img 
                :src="userProfile?.avatarUrl || defaultAvatar" 
                alt="User" 
                class="w-full h-full object-cover"
              />
            </button>
            <!-- Hide Name on Tablet/Mobile to save space -->
            <div class="hidden lg:flex flex-col items-start ml-0.5">
              <span class="text-xs md:text-sm font-bold text-[var(--text)] leading-tight max-w-[100px] truncate">
                {{ userProfile?.nickname || userProfile?.username || 'User' }}
              </span>
            </div>
          </div>
          <template #dropdown>
            <el-dropdown-menu class="glass-dropdown">
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>个人中心
              </el-dropdown-item>
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
      
      <!-- Login Button -->
      <button v-else @click="$emit('open-login')" class="text-xs md:text-sm font-bold text-white bg-indigo-600 px-3 md:px-4 py-1.5 md:py-2 rounded-full hover:bg-indigo-500 hover:shadow-lg hover:shadow-indigo-500/30 transition-all active:scale-95">
        登录
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { Play, Upload } from 'lucide-vue-next'
import { Search, Menu, User, Setting, SwitchButton, Management } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { userApi, type UserProfileVO } from '@/api'
import NotificationBell from './NotificationBell.vue'

const props = defineProps<{
  theme: 'light' | 'dark'
  isLoggedIn: boolean
}>()

const emit = defineEmits<{
  'toggle-theme': []
  'toggle-sidebar': []
  'open-login': []
  'logout': []
  'navigate': [view: 'home' | 'settings' | 'profile' | 'admin' | 'upload']
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