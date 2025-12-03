<template>
  <div id="app"
    class="h-screen w-full flex flex-col relative selection:bg-indigo-500 selection:text-white transition-colors duration-300">

    <!-- 顶部进度条 -->
    <div class="loading-bar" :style="{ width: loadPercent + '%', opacity: (isLoading && !isInitialLoad) ? 1 : 0 }"></div>

    <!-- Splash Screen -->
    <SplashScreen :show="isInitialLoad" :progress="loadPercent" />

    <!-- 顶部导航栏 -->
    <NavBar :theme="theme" :is-logged-in="isLoggedIn" @toggle-sidebar="toggleSidebar" @open-login="openLogin"
      @logout="handleLogout" @navigate="handleNavigation" />

    <div class="flex flex-1 pt-16 h-full overflow-hidden relative z-10">

      <!-- 左侧导航 (Sidebar) -->
      <Sidebar class="hidden md:flex" @change-tab="handleTabChange" @navigate="handleNavigation" />

      <!-- 主内容区 -->
      <main class="flex-1 overflow-y-auto pt-4 md:pt-8 scroll-smooth" ref="mainScroll">
        <VideoMasonry v-if="activeTab === 'home'" :videos="videos" @open-video="openVideo" />
        <Profile v-else-if="activeTab === 'profile'" />
        <Settings v-else-if="activeTab === 'settings'" :is-logged-in="isLoggedIn" :theme="theme" @toggle-theme="toggleTheme" @require-login="openLogin" />
      </main>
    </div>

    <!-- 沉浸式详情页 (Modal) -->
    <VideoDetail :video="currentVideo" @close="closeVideo" />

    <!-- Login Modal -->
    <transition name="modal">
      <div v-if="showLoginModal" class="fixed inset-0 z-[100]">
        <Login @login-success="handleLoginSuccess" @close="closeLogin" />
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import NavBar from './components/NavBar.vue'
import Sidebar from './components/Sidebar.vue'
import VideoMasonry from './components/VideoMasonry.vue'
import VideoDetail from './components/VideoDetail.vue'
import Login from './views/Login.vue'
import Profile from './views/Profile.vue'
import Settings from './views/Settings.vue'
import SplashScreen from './components/SplashScreen.vue'
import { authApi } from '@/api'

// 状态管理
const isLoading = ref(false)
const isInitialLoad = ref(true)
const loadPercent = ref(0)
const activeTab = ref('home')
const currentVideo = ref(null)
const showLoginModal = ref(false)
const isLoggedIn = ref(false)
const theme = ref<'light' | 'dark'>('dark') // Default to dark as per design
const sidebarOpen = ref(false)

// 静态数据
const mockVideos = [
  { id: 1, title: "沉浸式体验：在雨夜的东京街头漫步 8K HDR", author: "WalkJapan", views: "24万", date: "2天前", duration: "12:34", cover: "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&q=80", avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" },
  { id: 2, title: "Apple Vision Pro 深度评测：空间计算的未来？", author: "TechReview", views: "89万", date: "5小时前", duration: "18:20", cover: "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=800&q=80", avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Tech" },
  { id: 3, title: "【4K】2025 极简主义桌面搭建指南 Desk Setup", author: "Minimalist", views: "12万", date: "1周前", duration: "08:45", cover: "https://images.unsplash.com/photo-1486946255434-2466348c2166?w=800&q=80", avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Desk" },
  { id: 4, title: "Cyberpunk 2077: 往日之影 最终结局剧情向", author: "GameMaster", views: "55万", date: "3天前", duration: "45:10", cover: "https://images.unsplash.com/photo-1552820728-8b83bb6b773f?w=800&q=80", avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Gamer" },
  { id: 5, title: "十分钟学会 Vue 3 Composition API", author: "CodeWithMe", views: "6.7万", date: "2天前", duration: "10:05", cover: "https://images.unsplash.com/photo-1587620962725-abab7fe55159?w=800&q=80", avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Code" },
  { id: 6, title: "如何在家做出米其林级别的惠灵顿牛排", author: "ChefGordon", views: "102万", date: "1天前", duration: "15:30", cover: "https://images.unsplash.com/photo-1600891964092-4316c288032e?w=800&q=80", avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Food" },
  { id: 7, title: "冰岛环岛自驾游 Vlog ep.1", author: "TravelTheWorld", views: "33万", date: "4天前", duration: "22:15", cover: "https://images.unsplash.com/photo-1476610182048-b716b8518aae?w=800&q=80", avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Trip" },
  { id: 8, title: "Lo-Fi Hip Hop Radio - Beats to Relax/Study to", author: "LofiGirl", views: "Live", date: "直播中", duration: "Live", cover: "https://images.unsplash.com/photo-1516280440614-6697288d5d38?w=800&q=80", avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Music" }
]

const videos = ref(mockVideos)

// 方法
const triggerLoad = (cb?: () => void) => {
  isLoading.value = true
  loadPercent.value = 0
  let p = 0
  const timer = setInterval(() => {
    p += Math.random() * 15
    if (p > 90) {
      clearInterval(timer)
      loadPercent.value = 100
      setTimeout(() => {
        isLoading.value = false
        if (isInitialLoad.value) {
          isInitialLoad.value = false
        }
        loadPercent.value = 0
        if (cb) cb()
      }, 500)
    } else {
      loadPercent.value = p
    }
  }, 50)
}

const handleTabChange = (tabId: string) => {
  if (activeTab.value !== tabId) {
    triggerLoad(() => activeTab.value = tabId)
  }
}

const handleNavigation = (view: 'home' | 'settings' | 'profile') => {
  if (view === 'profile' && !isLoggedIn.value) {
    openLogin()
    return
  }

  if (view === 'home') {
    handleTabChange('home')
  } else if (view === 'settings') {
    activeTab.value = 'settings'
  } else if (view === 'profile') {
    activeTab.value = 'profile'
  }
}

const toggleTheme = () => {
  theme.value = theme.value === 'light' ? 'dark' : 'light'
  document.documentElement.setAttribute('data-theme', theme.value)
}

const openVideo = (video: any) => {
  triggerLoad(() => {
    currentVideo.value = video
  })
}

const closeVideo = () => {
  currentVideo.value = null
}

// Check for token on startup
const checkAuth = async () => {
  // 1. Check URL for token (OAuth callback)
  const urlParams = new URLSearchParams(window.location.search)
  const urlToken = urlParams.get('token')
  
  if (urlToken) {
    localStorage.setItem('token', urlToken)
    // Clean URL
    window.history.replaceState({}, '', window.location.pathname)
    isLoggedIn.value = true
  }

  // 2. Validate token
  const token = localStorage.getItem('token')
  if (token) {
    try {
      // Validate token with backend
      await authApi.validateToken(token)
      isLoggedIn.value = true
    } catch (error) {
      // Token invalid or expired
      console.warn('Token validation failed:', error)
      localStorage.removeItem('token')
      isLoggedIn.value = false
    }
  }
}

checkAuth()

const openLogin = () => {
  showLoginModal.value = true
}

const closeLogin = () => {
  showLoginModal.value = false
}

const handleLoginSuccess = () => {
  isLoggedIn.value = true
  showLoginModal.value = false
}

const handleLogout = async () => {
  try {
    await authApi.logout()
  } catch (e) {
    console.error('Logout failed:', e)
  } finally {
    isLoggedIn.value = false
    localStorage.removeItem('token')
    if (activeTab.value === 'profile') {
      handleNavigation('home')
    }
  }
}

const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
}

onMounted(() => {
  document.documentElement.setAttribute('data-theme', theme.value)
  triggerLoad()
  
  window.addEventListener('unauthorized', () => {
    isLoggedIn.value = false
    // Optionally open login modal or just reset state
    // showLoginModal.value = true 
  })
})
</script>

<style scoped>
/* Modal Transition */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>