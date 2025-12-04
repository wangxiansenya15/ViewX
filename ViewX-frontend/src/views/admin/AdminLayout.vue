<template>
  <div class="flex h-full bg-gray-100 dark:bg-gray-900 relative">
    <!-- Mobile Header -->
    <div class="md:hidden fixed top-0 left-0 right-0 h-16 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between px-4 z-30">
      <div class="flex items-center gap-2 font-bold text-gray-800 dark:text-white">
        <el-icon><Management /></el-icon>
        <span>管理后台</span>
      </div>
      <button @click="isSidebarOpen = !isSidebarOpen" class="p-2 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg">
        <Menu class="w-6 h-6" />
      </button>
    </div>

    <!-- Sidebar Backdrop -->
    <div 
      v-if="isSidebarOpen" 
      @click="isSidebarOpen = false"
      class="fixed inset-0 bg-black/50 z-40 md:hidden glass-effect"
    ></div>

    <!-- Admin Sidebar -->
    <aside 
      :class="[
        'fixed inset-y-0 left-0 z-50 w-64 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 flex flex-col transition-transform duration-300 md:relative md:translate-x-0',
        isSidebarOpen ? 'translate-x-0' : '-translate-x-full'
      ]"
    >
      <div class="p-6 border-b border-gray-200 dark:border-gray-700 hidden md:block">
        <h2 class="text-xl font-bold text-gray-800 dark:text-white flex items-center gap-2">
          <el-icon><Management /></el-icon>
          管理后台
        </h2>
      </div>
      
      <!-- Mobile Close Button -->
      <div class="md:hidden p-4 flex justify-end border-b border-gray-200 dark:border-gray-700">
        <button @click="isSidebarOpen = false" class="p-2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-white">
          <X class="w-6 h-6" />
        </button>
      </div>
      
      <nav class="flex-1 p-4 space-y-2">
        <button 
          v-for="item in menuItems" 
          :key="item.id"
          @click="handleMenuClick(item.id)"
          :class="[
            'w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors',
            currentView === item.id 
              ? 'bg-indigo-50 text-indigo-600 dark:bg-indigo-900/30 dark:text-indigo-400' 
              : 'text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700'
          ]"
        >
          <component :is="item.icon" class="w-5 h-5" />
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div class="p-4 border-t border-gray-200 dark:border-gray-700">
        <button 
          @click="$emit('back-to-home')"
          class="w-full flex items-center gap-3 px-4 py-3 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700 rounded-lg transition-colors"
        >
          <component :is="HomeIcon" class="w-5 h-5" />
          <span>返回首页</span>
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="flex-1 overflow-y-auto p-4 md:p-8 pt-20 md:pt-8 w-full">
      <transition name="fade" mode="out-in">
        <component :is="currentViewComponent" />
      </transition>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { 
  LayoutDashboard, 
  Users, 
  Video, 
  Home as HomeIcon,
  Menu,
  X
} from 'lucide-vue-next'
import { Management } from '@element-plus/icons-vue'

// Import sub-views
import Dashboard from './Dashboard.vue'
import UserManagement from './UserManagement.vue'
import VideoReview from './VideoReview.vue'

const emit = defineEmits(['back-to-home'])

const currentView = ref('dashboard')
const isSidebarOpen = ref(false)

const menuItems = [
  { id: 'dashboard', label: '仪表盘', icon: LayoutDashboard },
  { id: 'users', label: '用户管理', icon: Users },
  { id: 'videos', label: '视频审核', icon: Video },
]

const currentViewComponent = computed(() => {
  switch (currentView.value) {
    case 'dashboard': return Dashboard
    case 'users': return UserManagement
    case 'videos': return VideoReview
    default: return Dashboard
  }
})

const handleMenuClick = (viewId: string) => {
  currentView.value = viewId
  isSidebarOpen.value = false // Close sidebar on mobile after selection
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
