<template>
  <aside
    class="w-20 lg:w-64 hidden md:flex flex-col glass-panel border-t-0 border-b-0 border-r border-[var(--border)] pt-6 pb-6 gap-6 z-20 h-full">

    <nav class="flex flex-col px-3 gap-1">
      <a v-for="item in navItems" :key="item.id" @click="activeTab = item.id; $emit('change-tab', item.id)"
        :class="['flex items-center gap-4 px-4 py-3 rounded-xl transition-all cursor-pointer group mb-1', 
            activeTab === item.id ? 'bg-[var(--primary)] text-white font-medium shadow-lg' : 'text-[var(--muted)] hover:bg-[var(--bg)] hover:text-[var(--text)]']">
        <component :is="item.iconComponent"
          :class="['w-5 h-5 transition-transform group-hover:scale-110', activeTab === item.id ? 'text-white' : '']" />
        <span class="hidden lg:block text-sm">{{ t(item.nameKey) }}</span>
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
        class="flex items-center gap-4 px-4 py-3 rounded-xl text-[var(--muted)] hover:bg-[var(--bg)] hover:text-[var(--text)] transition-all cursor-pointer group">
        <Settings class="w-5 h-5 transition-transform group-hover:rotate-90" />
        <span class="hidden lg:block text-sm">{{ t('sidebar.settings') }}</span>
      </a>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Home, Flame, Users, Library, Sparkles, Settings, User } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const activeTab = ref('home')

const navItems = [
  { id: 'home', nameKey: 'sidebar.home', iconComponent: Home },
  { id: 'trending', nameKey: 'sidebar.trending', iconComponent: Flame },
  { id: 'subs', nameKey: 'sidebar.subs', iconComponent: Users },
  { id: 'library', nameKey: 'sidebar.library', iconComponent: Library },
  { id: 'profile', nameKey: 'sidebar.me', iconComponent: User },
]

defineEmits(['change-tab', 'navigate'])
</script>