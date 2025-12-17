<template>
  <header class="fixed top-0 left-0 right-0 z-20 flex items-center justify-between px-4 py-3 bg-gradient-to-b from-black/80 to-transparent pointer-events-none transition-all duration-300">
    <!-- Left: Sidebar Trigger -->
    <button @click="$emit('toggle-sidebar')" class="pointer-events-auto p-2 rounded-full active:bg-white/10 text-white transition-colors">
      <LayoutGrid class="w-6 h-6 shadow-sm" />
    </button>

    <!-- Center: Tabs -->
    <div class="pointer-events-auto flex items-center gap-6 text-base font-medium text-white/70 shadow-sm">
      <button 
        @click="$emit('change-tab', 'recommend')"
        :class="activeTab === 'recommend' ? 'text-white scale-105 font-bold' : 'hover:text-white'"
        class="transition-all relative"
      >
        推荐
        <div v-if="activeTab === 'recommend'" class="absolute -bottom-1.5 left-1/2 -translate-x-1/2 w-4 h-0.5 bg-white rounded-full"></div>
      </button>
      <button 
        @click="$emit('change-tab', 'follow')"
        :class="activeTab === 'follow' ? 'text-white scale-105 font-bold' : 'hover:text-white'"
        class="transition-all relative"
      >
        关注
        <div v-if="activeTab === 'follow'" class="absolute -bottom-1.5 left-1/2 -translate-x-1/2 w-4 h-0.5 bg-white rounded-full"></div>
      </button>
    </div>

    <!-- Right: Actions -->
    <div class="pointer-events-auto flex items-center gap-4 text-white">
      <button @click="$router.push('/search')" class="p-1 rounded-full active:bg-white/10 transition-colors">
        <Search class="w-6 h-6 shadow-sm" />
      </button>
      <!-- Notifications -->
      <button @click="$router.push('/notifications')" class="p-1 rounded-full active:bg-white/10 relative transition-colors">
        <Bell class="w-6 h-6 shadow-sm" />
        <span class="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full border border-black/50"></span>
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { LayoutGrid, Search, Bell } from 'lucide-vue-next'

withDefaults(defineProps<{
  activeTab?: string,
  userAvatar?: string
}>(), {
  activeTab: 'recommend'
})

defineEmits(['toggle-sidebar', 'change-tab', 'open-search'])
</script>
