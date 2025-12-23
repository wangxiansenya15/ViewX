<template>
  <nav class="fixed bottom-0 left-0 right-0 z-30 bg-black/90 backdrop-blur-xl border-t border-white/5 pb-safe pt-1.5">
     <div class="flex items-center justify-between px-4 pb-1">
        <!-- Home -->
        <button @click="$router.push('/')" class="flex flex-col items-center gap-1 flex-1 transition-all" :class="activeRoute === 'home' ? 'text-white' : 'text-gray-400'">
          <Home class="w-6 h-6" :class="activeRoute === 'home' ? 'fill-current' : ''" />
          <span class="text-[10px] font-medium">首页</span>
        </button>

        <!-- Upload -->
        <button @click="$router.push('/upload')" class="flex flex-col items-center gap-1 flex-1 transition-all text-white active:scale-95">
           <div class="w-10 h-7 rounded-xl bg-gradient-to-r from-indigo-500 to-purple-600 flex items-center justify-center shadow-lg shadow-indigo-500/20 mt-0.5">
               <Plus class="w-5 h-5 text-white" />
           </div>
           <span class="text-[10px] font-medium text-gray-200">投稿</span>
        </button>

        <!-- Messages -->
        <button @click="$router.push('/messages')" class="flex flex-col items-center gap-1 flex-1 transition-all" :class="activeRoute === 'messages' ? 'text-white' : 'text-gray-400'">
           <MessageCircle class="w-6 h-6" :class="activeRoute === 'messages' ? 'fill-current' : ''" />
           <span class="text-[10px] font-medium">消息</span>
        </button>

        <!-- Profile -->
        <button @click="$router.push('/profile')" class="flex flex-col items-center gap-1 flex-1 transition-all" :class="activeRoute === 'profile' ? 'text-white' : 'text-gray-400'">
           <User class="w-6 h-6" :class="activeRoute === 'profile' ? 'fill-current' : ''" />
           <span class="text-[10px]">个人中心</span>
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
  padding-bottom: env(safe-area-inset-bottom, 12px);
}
</style>
