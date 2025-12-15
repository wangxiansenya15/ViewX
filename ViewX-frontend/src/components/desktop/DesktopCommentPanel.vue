<template>
  <div class="h-full flex flex-col bg-[#161616] border-l border-white/10">
    <!-- Header -->
    <div class="p-4 border-b border-white/10 flex items-center justify-between">
       <div class="text-sm font-bold text-white">
         评论 <span class="text-gray-500 font-normal text-xs ml-1">10.2w</span>
       </div>
       <button @click="$emit('close')" class="hover:bg-white/10 p-1 rounded-full transition-colors">
          <X class="w-5 h-5 text-gray-500" />
       </button>
    </div>

    <!-- Comments List -->
    <div class="flex-1 overflow-y-auto p-4 space-y-6 scrollbar-thin scrollbar-thumb-white/20 hover:scrollbar-thumb-white/30">
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
             <Heart class="w-4 h-4 text-gray-500 hover:text-red-500 cursor-pointer" />
             <span class="text-[10px] text-gray-500">123</span>
          </div>
       </div>
    </div>

    <!-- Input -->
    <div class="p-4 border-t border-white/10 bg-[#1f1f1f]">
       <div class="flex items-center gap-3">
           <div class="w-8 h-8 rounded-full bg-gray-700 shrink-0"></div>
           <div class="flex-1 bg-white/5 rounded-full px-4 py-2 flex items-center">
              <input type="text" placeholder="留下你的精彩评论..." class="bg-transparent border-none text-sm text-white placeholder-gray-500 w-full focus:outline-none" />
           </div>
           <button class="p-2 text-indigo-500 font-bold hover:bg-white/5 rounded-full">
              <Send class="w-5 h-5" />
           </button>
       </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { X, Heart, Send } from 'lucide-vue-next'

defineEmits(['close'])
</script>
