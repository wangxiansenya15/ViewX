<template>
  <div class="relative z-[50]">
    <!-- Backdrop -->
    <transition name="fade">
      <div v-if="isOpen" @click="$emit('close')" class="fixed inset-0 bg-black/60 backdrop-blur-sm"></div>
    </transition>

    <!-- Sidebar -->
    <transition name="slide-right">
      <div v-if="isOpen" class="fixed left-0 top-0 bottom-0 w-[75%] max-w-[320px] bg-[#161616] flex flex-col p-6 shadow-2xl border-r border-white/5">
        <!-- User Header -->
        <div class="flex items-center gap-4 mb-8 pb-8 border-b border-white/5">
           <div class="w-14 h-14 rounded-full bg-gray-800 overflow-hidden border-2 border-indigo-500 shadow-[0_0_15px_rgba(99,102,241,0.3)]">
              <!-- Placeholder Avatar -->
              <div class="w-full h-full flex items-center justify-center text-white bg-gradient-to-br from-indigo-500 to-purple-600 font-bold text-xl">A</div>
           </div>
           <div class="flex-1">
              <h2 class="text-white font-bold text-lg">Arthur</h2>
              <div class="flex items-center gap-2 text-xs text-gray-400 mt-1">
                 <span>UID: 8848</span>
                 <span class="px-1.5 py-0.5 bg-yellow-500/20 text-yellow-500 rounded text-[10px] font-bold">LV.6</span>
              </div>
           </div>
        </div>

        <!-- Menu Groups -->
        <div class="flex-1 overflow-y-auto scrollbar-hide space-y-6">
           <!-- Tools -->
           <div>
              <div class="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2 px-3">常用功能</div>
              <div class="space-y-1">
                 <button class="w-full flex items-center gap-3 p-3 rounded-xl hover:bg-white/5 text-gray-200 hover:text-white transition-colors">
                    <History class="w-5 h-5 text-gray-400" />
                    <span class="font-medium">观看历史</span>
                 </button>
                 <button class="w-full flex items-center gap-3 p-3 rounded-xl hover:bg-white/5 text-gray-200 hover:text-white transition-colors">
                    <Download class="w-5 h-5 text-gray-400" />
                    <span class="font-medium">离线缓存</span>
                 </button>
                 <button class="w-full flex items-center gap-3 p-3 rounded-xl hover:bg-white/5 text-gray-200 hover:text-white transition-colors">
                    <Clock class="w-5 h-5 text-gray-400" />
                    <span class="font-medium">稍后再看</span>
                 </button>
              </div>
           </div>

           <!-- Categories -->
           <div>
              <div class="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2 px-3">频道分区</div>
              <div class="grid grid-cols-2 gap-2">
                 <button v-for="cat in categories" :key="cat" class="flex items-center gap-2 p-3 rounded-xl bg-white/5 hover:bg-white/10 text-gray-300 hover:text-white transition-colors text-sm">
                    <Hash class="w-4 h-4 text-indigo-500" />
                    <span>{{ cat }}</span>
                 </button>
              </div>
           </div>
        </div>

        <!-- Bottom Actions -->
        <div class="pt-6 border-t border-white/5 mt-auto flex items-center gap-2">
           <button class="flex-1 flex items-center justify-center gap-2 p-3 rounded-xl bg-white/5 hover:bg-white/10 text-sm font-medium text-white transition-colors" @click="$router.push('/settings')">
              <Settings class="w-4 h-4" />
              设置
           </button>
           <button class="flex-1 flex items-center justify-center gap-2 p-3 rounded-xl bg-white/5 hover:bg-white/10 text-sm font-medium text-white transition-colors">
              <HelpCircle class="w-4 h-4" />
              帮助
           </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { Settings, Hash, History, Download, Clock, HelpCircle } from 'lucide-vue-next'

defineProps<{
  isOpen: boolean
}>()

defineEmits(['close'])

const categories = ['科技数码', '游戏实况', '影视解说', '生活', '美食', '音乐', '动画']
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(-100%);
}
</style>
