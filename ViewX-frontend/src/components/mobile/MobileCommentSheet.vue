<template>
  <div class="relative z-[60]">
    <!-- Backdrop -->
    <transition name="fade">
      <div v-if="isOpen" @click="$emit('close')" class="fixed inset-0 bg-black/50 backdrop-blur-sm"></div>
    </transition>

    <!-- Bottom Sheet -->
    <transition name="slide-up">
      <div v-if="isOpen" class="fixed left-0 right-0 bottom-0 h-[70vh] bg-[#161616] rounded-t-3xl shadow-2xl flex flex-col border-t border-white/10">
        <!-- Handle -->
        <div class="w-full flex items-center justify-center pt-3 pb-2 cursor-grab active:cursor-grabbing" @click="$emit('close')">
           <div class="w-12 h-1.5 bg-white/20 rounded-full"></div>
        </div>

        <!-- Header -->
        <div class="bg-[#161616] z-10 sticky top-0 px-4 py-2 flex items-center justify-between border-b border-white/5">
           <div class="text-sm font-bold text-white">
             评论 <span class="text-gray-500 font-normal text-xs ml-1">10.2w</span>
           </div>
           <button @click="$emit('close')">
              <X class="w-5 h-5 text-gray-500" />
           </button>
        </div>

        <!-- Comments List -->
        <div class="flex-1 overflow-y-auto p-4 space-y-6">
           <div v-for="i in 10" :key="i" class="flex gap-3">
              <div class="w-9 h-9 rounded-full bg-gray-700 shrink-0"></div>
              <div class="flex-1">
                 <div class="text-xs text-gray-400 font-bold mb-1">User_{{ i }}</div>
                 <div class="text-sm text-gray-200 leading-relaxed">
                   This UI is absolutely stunning! Loving the new dark mode aesthetics. 🔥
                 </div>
                 <div class="flex items-center gap-4 mt-2 text-xs text-gray-500">
                    <span>10-23</span>
                    <button class="font-bold hover:text-white">Reply</button>
                 </div>
              </div>
              <div class="flex flex-col items-center gap-1">
                 <Heart class="w-4 h-4 text-gray-500" />
                 <span class="text-[10px] text-gray-500">123</span>
              </div>
           </div>
        </div>

        <!-- Input -->
        <div class="p-3 border-t border-white/10 bg-[#1f1f1f] pb-safe flex items-center gap-3">
           <div class="w-8 h-8 rounded-full bg-gray-700"></div>
           <div class="flex-1 bg-white/5 rounded-full px-4 py-2 flex items-center text-gray-400 text-sm">
              留下你的精彩评论...
           </div>
           <button class="p-2 text-indigo-500 font-bold">
              <Send class="w-5 h-5" />
           </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { X, Heart, Send } from 'lucide-vue-next'

defineProps<{
  isOpen: boolean
}>()

defineEmits(['close'])
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

.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
}

.pb-safe {
  padding-bottom: max(12px, env(safe-area-inset-bottom, 12px));
}
</style>
