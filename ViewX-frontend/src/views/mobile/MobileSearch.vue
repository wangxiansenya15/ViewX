<template>
  <div class="fixed inset-0 bg-black z-50 flex flex-col text-white">
    <!-- Header -->
    <div class="flex items-center gap-3 p-4 border-b border-white/5">
       <button @click="$router.back()" class="text-white p-1 active:bg-white/10 rounded-full">
          <ArrowLeft class="w-6 h-6" />
       </button>
       <div class="flex-1 relative">
         <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
         <input 
           ref="inputRef"
           v-model="query"
           type="text" 
           placeholder="搜索视频、用户..." 
           class="w-full bg-[#1f1f1f] text-white rounded-full py-2 pl-10 pr-10 text-sm focus:outline-none focus:ring-1 focus:ring-indigo-500 border border-transparent focus:border-indigo-500/50"
           @keyup.enter="handleSearch"
         />
         <button v-if="query" @click="query = ''" class="absolute right-3 top-1/2 -translate-y-1/2 rounded-full bg-gray-700 p-0.5">
           <X class="w-3 h-3 text-gray-300" />
         </button>
       </div>
       <button @click="handleSearch" class="text-indigo-500 font-bold text-sm active:opacity-70">搜索</button>
    </div>

    <!-- Content (If no query active) -->
    <div class="flex-1 overflow-y-auto p-4 scrollbar-hide">
       <!-- History -->
       <div v-if="history.length" class="mb-8">
          <div class="flex items-center justify-between mb-3 text-sm text-gray-400 font-medium">
             <span>历史记录</span>
             <button @click="clearHistory" class="p-1 hover:text-white transition-colors"><Trash2 class="w-4 h-4" /></button>
          </div>
          <div class="flex flex-wrap gap-2">
             <span v-for="item in history" :key="item" @click="search(item)" class="px-3 py-1.5 bg-white/5 rounded-full text-xs text-gray-300 active:bg-white/20 transition-colors cursor-pointer border border-white/5">
               {{ item }}
             </span>
          </div>
       </div>

       <!-- Trending -->
       <div>
          <div class="flex items-center gap-2 mb-4">
             <Flame class="w-4 h-4 text-red-500" />
             <span class="text-sm font-bold text-gray-200">ViewX 热搜榜</span>
          </div>
          <div class="grid grid-cols-1 gap-1">
             <div v-for="(item, idx) in trending" :key="item.keyword" @click="search(item.keyword)" class="flex items-center justify-between p-3 rounded-xl active:bg-white/5 cursor-pointer transition-colors">
                <div class="flex items-center gap-3">
                   <span :class="{'text-[#ffeb3b]': idx === 0, 'text-[#c0c0c0]': idx === 1, 'text-[#cd7f32]': idx === 2, 'text-gray-500': idx > 2}" class="font-bold w-4 text-center text-sm italic">{{ idx + 1 }}</span>
                   <span class="text-gray-200 text-sm font-medium">{{ item.keyword }}</span>
                </div>
                <div class="flex items-center gap-1">
                   <span v-if="item.hot" class="text-[10px] bg-red-500/10 text-red-500 px-1.5 py-0.5 rounded ml-2 font-bold">热</span>
                   <span v-if="item.new" class="text-[10px] bg-yellow-500/10 text-yellow-500 px-1.5 py-0.5 rounded ml-2 font-bold">新</span>
                   <span class="text-xs text-gray-600">{{ formatCount(item.count) }}</span>
                </div>
             </div>
          </div>
       </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ArrowLeft, Search, X, Trash2, Flame } from 'lucide-vue-next'
// import { useRouter } from 'vue-router'

// const router = useRouter() - unused
const query = ref('')
const inputRef = ref<HTMLInputElement | null>(null)
const history = ref<string[]>(['PS5 Pro Review', 'Vue 3 Tutorial', 'Deepmind', 'SpaceX'])
const trending = ref([
  { keyword: '原神 4.2', hot: true, new: false, count: 1250000 },
  { keyword: 'iPhone 16 Pro Max', hot: true, new: true, count: 980000 },
  { keyword: 'GTA 6 Trailer', hot: true, new: false, count: 850000 },
  { keyword: 'Vue.js 3.4', hot: false, new: true, count: 420000 },
  { keyword: 'Cyberpunk DLC', hot: false, new: false, count: 310000 },
  { keyword: 'React vs Vue', hot: false, new: false, count: 280000 },
])

onMounted(() => {
  inputRef.value?.focus()
})

const handleSearch = () => {
  if (query.value.trim()) {
    search(query.value)
  }
}

const search = (q: string) => {
  query.value = q
  if (!history.value.includes(q)) history.value.unshift(q)
  // Navigate to results or show results component
  console.log('Searching for:', q)
}

const clearHistory = () => {
  history.value = []
}

const formatCount = (num: number) => {
  if (num > 10000) return (num / 10000).toFixed(1) + 'w'
  return num
}
</script>

<style scoped>
.scrollbar-hide::-webkit-scrollbar {
    display: none;
}
</style>
