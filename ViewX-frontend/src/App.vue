<template>
  <div id="app" class="h-screen w-full flex flex-col relative selection:bg-indigo-500 selection:text-white bg-[var(--bg)] text-[var(--text)] overflow-hidden transition-colors duration-300">
    
    <!-- Global Loading -->
    <div class="loading-bar" :style="{ width: loadPercent + '%', opacity: (isLoading && !isInitialLoad) ? 1 : 0 }"></div>
    <SplashScreen :show="isInitialLoad" :progress="loadPercent" />

    <!-- === MOBILE LAYOUT === -->
    <div v-if="isMobile" class="h-full w-full relative z-0">
       <!-- Header (Conditional) -->
       <MobileHeader 
         v-if="!route.meta.hideHeader" 
         :active-tab="mobileTab"
         @toggle-sidebar="mobileSidebarOpen = true"
         @change-tab="(t: string) => mobileTab = t"
       />

       <!-- Main Content -->
       <div class="h-full w-full relative overflow-y-auto no-scrollbar scroll-smooth">
         <router-view v-slot="{ Component }">
            <keep-alive>
               <component :is="Component" :theme="theme" :isLoggedIn="isLoggedIn" @toggle-theme="toggleTheme" />
            </keep-alive>
         </router-view>
       </div>

       <!-- Bottom Nav (Conditional) -->
       <MobileBottomNav v-if="!route.meta.hideNav" />

       <!-- Overlays -->
       <MobileSidebar :is-open="mobileSidebarOpen" @close="mobileSidebarOpen = false" />
       <MobileCommentSheet :is-open="mobileCommentsOpen" @close="mobileCommentsOpen = false" />
    </div>

    <!-- === DESKTOP LAYOUT === -->
    <div v-else class="h-full w-full flex flex-col relative z-0">
       <NavBar 
         v-if="!route.meta.hideHeader"
         :theme="theme" 
         :is-logged-in="isLoggedIn" 
         @toggle-sidebar="desktopSidebarOpen = !desktopSidebarOpen" 
         @open-login="showLoginModal = true"
         @logout="handleLogout" 
         @navigate="handleDesktopNavigation"
         @toggle-theme="toggleTheme"
       />

       <div :class="['flex flex-1 overflow-hidden relative z-10', !route.meta.hideHeader ? 'pt-14 md:pt-16' : '']">
         <Sidebar class="hidden md:flex" v-show="desktopSidebarOpen && !route.meta.hideSidebar" 
           @change-tab="handleDesktopTabChange"
           @navigate="handleDesktopNavigation"
         />
         
         <main class="flex-1 overflow-y-auto scroll-smooth relative" id="desktop-main">
            <router-view v-slot="{ Component }">
               <component :is="Component" :theme="theme" :isLoggedIn="isLoggedIn" @toggle-theme="toggleTheme" />
            </router-view>
         </main>
       </div>
    </div>

    <!-- Shared Modals -->
    <VideoDetail v-if="currentVideo" :video="currentVideo" @close="currentVideo = null" />
    
    <transition name="modal">
      <div v-if="showLoginModal" class="fixed inset-0 z-[100]">
        <Login @login-success="handleLoginSuccess" @close="showLoginModal = false" />
      </div>
    </transition>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, provide, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { type VideoVO } from '@/api'
import { useWindowSize } from '@vueuse/core'
import { getVersionChecker, destroyVersionChecker } from '@/utils/versionChecker'

// Components
import NavBar from './components/NavBar.vue'
import Sidebar from './components/Sidebar.vue'
import VideoDetail from './components/VideoDetail.vue'
import SplashScreen from './components/SplashScreen.vue'
import Login from './views/Login.vue'

// Mobile Components
import MobileHeader from './components/mobile/MobileHeader.vue'
import MobileBottomNav from './components/mobile/MobileBottomNav.vue'
import MobileSidebar from './components/mobile/MobileSidebar.vue'
import MobileCommentSheet from './components/mobile/MobileCommentSheet.vue'

const route = useRoute()
const router = useRouter()
const { width } = useWindowSize()

// State
const isMobile = computed(() => width.value < 768)
const theme = ref<'light' | 'dark'>('dark')
const isLoggedIn = ref(false)
const isLoading = ref(false)
const isInitialLoad = ref(true)
const loadPercent = ref(0) // Mock loading

// Layout State
const mobileSidebarOpen = ref(false)
const mobileCommentsOpen = ref(false)
const mobileTab = ref('recommend')
const desktopSidebarOpen = ref(true)

// Modal State
const currentVideo = ref<VideoVO | null>(null)
const showLoginModal = ref(false)

// === ACTIONS PROVIDED TO CHILDREN ===
const openComments = (_video: VideoVO) => {
  if (isMobile.value) {
    mobileCommentsOpen.value = true
  }
}

const openDesktopVideo = (video: VideoVO) => {
  currentVideo.value = video
}

const toggleTheme = () => {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem('theme', theme.value)
}

// Watch theme changes
watch(theme, (newTheme) => {
  document.documentElement.setAttribute('data-theme', newTheme)
}, { immediate: true })

provide('isMobile', isMobile)
provide('openComments', openComments)
provide('openDesktopVideo', openDesktopVideo)
provide('isLoggedIn', isLoggedIn)
provide('theme', theme)
provide('toggleTheme', toggleTheme)

// === AUTH & LOAD LOGIC ===
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
        if (isInitialLoad.value) isInitialLoad.value = false
        if (cb) cb()
      }, 500)
    } else {
      loadPercent.value = p
    }
  }, 50)
}

const checkAuth = async () => {
  const token = localStorage.getItem('token')
  if (token) isLoggedIn.value = true
}

const handleLogout = async () => {
  localStorage.removeItem('token')
  isLoggedIn.value = false
  router.push('/')
}

const handleLoginSuccess = () => {
  isLoggedIn.value = true
  showLoginModal.value = false
}

const handleDesktopNavigation = (view: string) => {
  switch (view) {
    case 'home':
      router.push('/')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'profile':
      if (isLoggedIn.value) router.push('/profile')
      else showLoginModal.value = true
      break
    case 'admin':
      router.push('/admin')
      break
    case 'upload':
      if (isLoggedIn.value) router.push('/upload')
      else showLoginModal.value = true
      break
    default:
      console.warn('Unknown view:', view)
  }
}

const handleDesktopTabChange = (tabId: string) => {
  // Sidebar tabs mapping
  if (tabId === 'home') router.push('/')
  else if (tabId === 'profile') {
    if (isLoggedIn.value) router.push('/profile')
    else showLoginModal.value = true
  }
  else if (tabId === 'trending') router.push('/trending') // Ensure route exists or handle appropriately
  else if (tabId === 'subs') router.push('/subscriptions')
  else if (tabId === 'library') router.push('/library')
}

onMounted(() => {
  checkAuth()
  triggerLoad()
  
  // init theme
  const saved = localStorage.getItem('theme') as 'light' | 'dark' | null
  if (saved) {
    theme.value = saved
  }
  
  // init version checker
  const appVersion = import.meta.env.VITE_APP_VERSION || '1.0.0'
  const versionChecker = getVersionChecker(appVersion)
  versionChecker.init()
  
  // Listen for auth state changes (e.g., from OAuth callback)
  window.addEventListener('auth-state-changed', handleAuthStateChange)
})

onUnmounted(() => {
  destroyVersionChecker()
  window.removeEventListener('auth-state-changed', handleAuthStateChange)
})

// Handle auth state change event
const handleAuthStateChange = (event: Event) => {
  const customEvent = event as CustomEvent
  if (customEvent.detail?.isLoggedIn) {
    checkAuth()
  }
}
</script>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.3s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.loading-bar { height: 3px; background: #6366f1; position: fixed; top: 0; left: 0; z-index: 9999; transition: width 0.2s, opacity 0.4s; }
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
</style>