<template>
  <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm" @click.self="close">
    <div class="bg-[#1a1a1a] border border-white/10 rounded-2xl w-full max-w-md overflow-hidden shadow-2xl flex flex-col max-h-[80vh]">
      <!-- Header -->
      <div class="p-4 border-b border-white/10 flex justify-between items-center shrink-0">
        <h3 class="text-lg font-bold text-white">{{ title }}</h3>
        <button @click="close" class="text-gray-400 hover:text-white p-1">
          <X class="w-6 h-6" />
        </button>
      </div>

      <!-- List -->
      <div class="flex-1 overflow-y-auto p-0" @scroll="checkScroll">
        <div v-if="loading && list.length === 0" class="flex justify-center p-8">
            <Loader2 class="w-8 h-8 animate-spin text-indigo-500" />
        </div>
        
        <div v-else-if="list.length > 0" class="divide-y divide-white/5">
            <div v-for="user in list" :key="user.id" class="flex items-center justify-between p-4 hover:bg-white/5 transition-colors cursor-pointer" @click="goToProfile(user.id)">
                <div class="flex items-center gap-3 flex-1 overflow-hidden">
                    <img :src="user.avatar || defaultAvatar" class="w-10 h-10 rounded-full bg-gray-700 object-cover border border-white/10" />
                    <div class="truncate">
                        <div class="font-medium text-white truncate">{{ user.nickname || user.username }}</div>
                        <div class="text-xs text-gray-500 truncate">@{{ user.username }}</div>
                    </div>
                </div>
                
                <button 
                    @click.stop="toggleFollow(user)"
                    class="ml-3 px-4 py-1.5 rounded-full text-xs font-medium transition-all"
                    :class="user.isFollowing 
                        ? 'bg-white/10 text-white hover:bg-white/20' 
                        : 'bg-indigo-600 text-white hover:bg-indigo-700'"
                >
                    {{ user.isFollowing ? '已关注' : '关注' }}
                </button>
            </div>
            
             <!-- Load More Loader -->
             <div v-if="loadingMore" class="flex justify-center p-4">
                 <Loader2 class="w-5 h-5 animate-spin text-gray-500" />
             </div>
        </div>
        
        <div v-else class="flex flex-col items-center justify-center py-12 text-gray-500">
            <Users class="w-12 h-12 mb-3 opacity-20" />
            <p>暂无用户</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { X, Loader2, Users } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { interactionApi, type UserSummaryVO } from '@/api'

const props = defineProps<{
    visible: boolean
    title: string
    userId: number
    type: 'followers' | 'following'
}>()

const emit = defineEmits(['update:visible'])

const router = useRouter()
const list = ref<UserSummaryVO[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(1)
const hasMore = ref(true)
const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'

const close = () => emit('update:visible', false)

const loadData = async (isLoadMore = false) => {
    if ((isLoadMore && (!hasMore.value || loadingMore.value)) || (!isLoadMore && loading.value)) return
    
    if (isLoadMore) loadingMore.value = true
    else loading.value = true
    
    try {
        const fetchFn = props.type === 'followers' ? interactionApi.getFollowers : interactionApi.getFollowing
        const res = await fetchFn(props.userId, page.value, 20)
        
        if (isLoadMore) {
            list.value.push(...res)
        } else {
            list.value = res
        }
        
        hasMore.value = res.length === 20
        if (hasMore.value) page.value++
    } catch (e) {
        console.error('Failed to load users', e)
    } finally {
        loading.value = false
        loadingMore.value = false
    }
}

watch(() => props.visible, (val) => {
    if (val) {
        page.value = 1
        hasMore.value = true
        list.value = []
        loadData()
    }
})

const checkScroll = (e: Event) => {
    const el = e.target as HTMLElement
    if (el.scrollHeight - el.scrollTop <= el.clientHeight + 50) {
        loadData(true)
    }
}

const toggleFollow = async (user: UserSummaryVO) => {
    try {
        // Optimistic update
        user.isFollowing = !user.isFollowing
        await interactionApi.toggleFollow(user.id)
    } catch (e) {
        user.isFollowing = !user.isFollowing // Revert
        console.error('Toggle follow failed', e)
    }
}

const goToProfile = (id: number) => {
    close()
    router.push(`/profile/${id}`)
}
</script>
