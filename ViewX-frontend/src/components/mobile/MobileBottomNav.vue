<template>
  <nav class="fixed bottom-0 left-0 right-0 z-30 bg-black/90 backdrop-blur-xl border-t border-white/5 pb-safe pt-2">
     <div class="flex items-center justify-between px-4 pb-2">
        <!-- Home -->
        <!-- Home -->
        <button @click="$router.push('/')" class="flex flex-col items-center gap-1.5 flex-1 transition-all" :class="activeRoute === 'home' ? 'text-white' : 'text-gray-400'">
          <Home class="w-7 h-7" :class="activeRoute === 'home' ? 'fill-current' : ''" />
          <span class="text-xs font-medium">首页</span>
        </button>

        <!-- Upload -->
        <!-- Upload -->
        <button @click="$router.push('/upload')" class="flex flex-col items-center gap-1.5 flex-1 transition-all text-white active:scale-95">
           <div class="w-14 h-9 rounded-2xl bg-gradient-to-r from-indigo-500 to-purple-600 flex items-center justify-center shadow-lg shadow-indigo-500/20">
               <Plus class="w-7 h-7 text-white" />
           </div>
           <span class="text-xs font-medium text-gray-200">投稿</span>
        </button>

        <!-- Messages -->
        <button @click="$router.push('/messages')" class="flex flex-col items-center gap-1.5 flex-1 transition-all" :class="activeRoute === 'messages' ? 'text-white' : 'text-gray-400'">
           <MessageCircle class="w-7 h-7" :class="activeRoute === 'messages' ? 'fill-current' : ''" />
           <span class="text-xs font-medium">消息</span>
        </button>

        <!-- Profile -->
        <!-- Profile -->
        <button @click="$router.push('/profile')" class="flex flex-col items-center gap-1.5 flex-1 transition-all" :class="activeRoute === 'profile' ? 'text-white' : 'text-gray-400'">
           <User class="w-7 h-7" :class="activeRoute === 'profile' ? 'fill-current' : ''" />
           <span class="text-xs">个人中心</span>
        </button>
     </div>
  </nav>
</template>

<script setup lang="ts">
import { Home, Plus, MessageCircle, User } from 'lucide-vue-next'
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeRoute = computed(() => route.name)
</script>

<style scoped>
.pb-safe {
  padding-bottom: env(safe-area-inset-bottom, 20px);
}
</style>
