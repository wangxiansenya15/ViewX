<template>
  <aside
    class="w-20 lg:w-64 hidden md:flex flex-col glass-panel border-t-0 border-b-0 border-r border-[var(--border)] pt-6 pb-6 gap-6 z-20 h-full">

    <nav class="flex flex-col px-3 gap-1">
      <a v-for="item in navItems" :key="item.id" @click="handleNavClick(item)"
        :class="['flex items-center gap-4 px-4 py-3 rounded-xl transition-all cursor-pointer group mb-1', 
            activeTab === item.id ? 'bg-[var(--primary)] text-white font-medium shadow-lg' : 'text-[var(--muted)] hover:bg-[var(--bg)] hover:text-[var(--text)]']">
        <component :is="item.iconComponent"
          :class="['w-5 h-5 transition-transform group-hover:scale-110', activeTab === item.id ? 'text-white' : '']" />
        <span class="hidden lg:block text-sm flex-1">{{ t(item.nameKey) }}</span>
        
        <!-- Toggle View Mode (Only for Home and when active) -->
        <el-tooltip
          v-if="item.id === 'home' && activeTab === 'home'"
          :content="viewMode === 'feed' ? t('sidebar.switchToGrid') : t('sidebar.switchToFeed')"
          placement="right"
          :show-after="200"
        >
          <div 
               class="hidden lg:flex items-center justify-center p-1.5 hover:bg-white/20 rounded-md transition-colors z-20"
               @click.stop="toggleViewMode">
             <LayoutGrid v-if="viewMode === 'feed'" class="w-4 h-4 text-white/90" />
             <Smartphone v-else class="w-4 h-4 text-white/90" />
          </div>
        </el-tooltip>
      </a>
    </nav>

    <!-- AI Promo Card -->
    <div class="mt-auto px-4 hidden lg:block mb-4">
      <div
        class="glass-card p-4 rounded-2xl bg-gradient-to-br from-indigo-600/20 to-purple-600/10 border border-indigo-500/20">
        <div class="flex items-center gap-2 mb-2 text-indigo-300">
          <Sparkles class="w-4 h-4" />
          <span class="text-xs font-bold uppercase tracking-wider ai-gradient-text">{{ t('sidebar.gemini.title') }}</span>
        </div>
        <p class="text-xs text-[var(--text-secondary)] mb-3 leading-relaxed">{{ t('sidebar.gemini.desc') }}</p>
        <button
          class="w-full py-2 bg-white text-black text-xs font-bold rounded-lg hover:bg-gray-200 spring-transition">{{ t('sidebar.gemini.more') }}</button>
      </div>
    </div>

    <!-- Settings Button -->
    <div class="px-3 pb-4">
      <a @click="$emit('navigate', 'settings')"
        :class="['flex items-center gap-4 px-4 py-3 rounded-xl transition-all cursor-pointer group',
          activeTab === 'settings' ? 'bg-[var(--primary)] text-white font-medium shadow-lg' : 'text-[var(--muted)] hover:bg-[var(--bg)] hover:text-[var(--text)]']">
        <Settings :class="['w-5 h-5 transition-transform group-hover:rotate-90', activeTab === 'settings' ? 'text-white' : '']" />
        <span class="hidden lg:block text-sm">{{ t('sidebar.settings') }}</span>
      </a>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Home, Flame, Users, Library, Sparkles, Settings, User, LayoutGrid, Smartphone, Bell, MessageCircle } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'
import { useHomeViewMode } from '@/composables/useHomeViewMode'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const activeTab = ref('home')
const { viewMode, toggleViewMode } = useHomeViewMode()

const navItems = [
  { id: 'home', nameKey: 'sidebar.home', iconComponent: Home, path: '/' },
  { id: 'trending', nameKey: 'sidebar.trending', iconComponent: Flame, path: '/trending' },
  { id: 'subs', nameKey: 'sidebar.subs', iconComponent: Users, path: '/subscriptions' },
  { id: 'library', nameKey: 'sidebar.library', iconComponent: Library, path: '/library' },
  { id: 'profile', nameKey: 'sidebar.me', iconComponent: User, path: '/profile' },
  { id: 'notifications', nameKey: 'sidebar.notifications', iconComponent: Bell, path: '/notifications' },
  { id: 'messages', nameKey: 'sidebar.messages', iconComponent: MessageCircle, path: '/messages' },
]

// 处理导航点击
const handleNavClick = (item: typeof navItems[0]) => {
  activeTab.value = item.id
  router.push(item.path)
  emit('change-tab', item.id)
}

watch(() => route.path, (newPath) => {
  if (newPath === '/') activeTab.value = 'home'
  else if (newPath.startsWith('/trending')) activeTab.value = 'trending'
  else if (newPath.startsWith('/subscriptions')) activeTab.value = 'subs'
  else if (newPath.startsWith('/library')) activeTab.value = 'library'
  else if (newPath === '/profile') activeTab.value = 'profile' // 只匹配精确路径
  else if (newPath.startsWith('/notifications')) activeTab.value = 'notifications'
  else if (newPath.startsWith('/messages')) activeTab.value = 'messages'
  else if (newPath.startsWith('/settings')) activeTab.value = 'settings'
  else activeTab.value = ''
}, { immediate: true })

const emit = defineEmits(['change-tab', 'navigate'])
</script>