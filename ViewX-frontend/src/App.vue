<template>
  <div id="app"
    class="h-screen w-full flex flex-col relative selection:bg-indigo-500 selection:text-white transition-colors duration-300">

    <!-- 顶部进度条 -->
    <div class="loading-bar" :style="{ width: loadPercent + '%', opacity: (isLoading && !isInitialLoad) ? 1 : 0 }"></div>

    <!-- Splash Screen -->
    <SplashScreen :show="isInitialLoad" :progress="loadPercent" />

    <!-- Admin Layout -->
    <AdminLayout v-if="activeTab === 'admin'" @back-to-home="handleNavigation('home')" />

    <!-- User Layout -->
    <template v-else>
      <!-- 顶部导航栏 -->
      <NavBar :theme="theme" :is-logged-in="isLoggedIn" @toggle-sidebar="toggleSidebar" @open-login="openLogin"
        @logout="handleLogout" @navigate="handleNavigation" />

      <div class="flex flex-1 pt-16 h-full overflow-hidden relative z-10">

        <!-- 左侧导航 (Sidebar) -->
        <Sidebar class="hidden md:flex" @change-tab="handleTabChange" @navigate="handleNavigation" />

        <!-- 主内容区 -->
        <main class="flex-1 overflow-y-auto pt-4 md:pt-8 scroll-smooth" ref="mainScroll">
          <VideoMasonry v-if="activeTab === 'home'" :videos="videos" @open-video="openVideo" />
          <Profile v-else-if="activeTab === 'profile'" @open-video="openVideo" />
          <UploadVideo v-else-if="activeTab === 'upload'" @publish-success="handleNavigation('profile')" @navigate="handleNavigation" />
          <Settings v-else-if="activeTab === 'settings'" :is-logged-in="isLoggedIn" :theme="theme" @toggle-theme="toggleTheme" @require-login="openLogin" />
        </main>
      </div>
    </template>

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
import UploadVideo from './views/UploadVideo.vue'
import Settings from './views/Settings.vue'
import AdminLayout from './views/admin/AdminLayout.vue'
import SplashScreen from './components/SplashScreen.vue'
import { authApi, videoApi, type VideoVO } from '@/api'

// 状态管理
const isLoading = ref(false)
const isInitialLoad = ref(true)
const loadPercent = ref(0)
const activeTab = ref('home')
const currentVideo = ref<VideoVO | null>(null)
const showLoginModal = ref(false)
const isLoggedIn = ref(false)
const theme = ref<'light' | 'dark'>('dark') // Default to dark as per design
const sidebarOpen = ref(false)

const videos = ref<VideoVO[]>([])

const fetchVideos = async () => {
  try {
    const res = await videoApi.getFeed()
    videos.value = res
  } catch (e) {
    console.error('Failed to fetch videos:', e)
  }
}

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
    triggerLoad(() => {
      activeTab.value = tabId
      if (tabId === 'home') {
        fetchVideos()
      }
    })
  }
}

const handleNavigation = (view: 'home' | 'settings' | 'profile' | 'admin' | 'upload') => {
  if (view === 'profile' && !isLoggedIn.value) {
    openLogin()
    return
  }

  if (view === 'upload' && !isLoggedIn.value) {
    openLogin()
    return
  }

  if (view === 'home') {
    handleTabChange('home')
  } else if (view === 'settings') {
    activeTab.value = 'settings'
  } else if (view === 'profile') {
    activeTab.value = 'profile'
  } else if (view === 'admin') {
    activeTab.value = 'admin'
  } else if (view === 'upload') {
    activeTab.value = 'upload'
  }
}

const toggleTheme = () => {
  theme.value = theme.value === 'light' ? 'dark' : 'light'
  document.documentElement.setAttribute('data-theme', theme.value)
}

const openVideo = async (video: VideoVO) => {
  try {
    // 调用后端API获取完整的视频详情
    const detailData = await videoApi.getDetail(video.id)
    
    triggerLoad(() => {
      currentVideo.value = detailData
    })
  } catch (error) {
    console.error('Failed to fetch video detail:', error)
    // 如果获取失败，降级使用列表数据
    triggerLoad(() => {
      currentVideo.value = video
    })
  }
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
  triggerLoad(() => {
    fetchVideos()
  })
  
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