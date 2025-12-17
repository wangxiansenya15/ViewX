<template>
  <div class="search-bar" ref="searchBarRef">
    <div class="search-input-wrapper">
      <input
        v-model="keyword"
        type="text"
        class="search-input"
        :placeholder="placeholder"
        @keyup.enter="handleSearch"
        @input="handleInput"
        @focus="handleFocus"
      />
      <button class="search-btn" @click="handleSearch">
        <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
          <path d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"/>
        </svg>
      </button>
    </div>

    <!-- 搜索结果下拉 (简化为联想词) -->
    <div v-if="showResults && (users.length > 0 || videos.length > 0)" class="search-results">
      <!-- 视频标题联想 -->
      <div v-if="videos.length > 0" class="group-list">
        <div class="group-title">相关视频</div>
        <div
          v-for="video in videos"
          :key="'v'+video.id"
          class="suggestion-item"
          @click="selectSuggestion(video.title)"
        >
          <svg class="w-4 h-4 mr-3 text-[var(--muted)]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
          <span class="suggestion-text">{{ video.title }}</span>
        </div>
      </div>
      
      <!-- 用户昵称联想 -->
      <div v-if="users.length > 0" class="group-list">
        <div class="group-title">相关用户</div>
        <div
          v-for="user in users"
          :key="'u'+user.id"
          class="suggestion-item"
          @click="selectSuggestion(user.nickname || user.username)"
        >
          <svg class="w-4 h-4 mr-3 text-[var(--muted)]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
          <span class="suggestion-text">{{ user.nickname || user.username }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { search, type UserSummaryVO, type VideoListVO } from '@/api'
import { debounce } from 'lodash-es'

// API 可能会有变动，这里使用直接导入的 search 对象
const searchApi = search

const props = withDefaults(defineProps<{
  placeholder?: string
  autoSearch?: boolean  // 是否自动搜索（输入时）
}>(), {
  placeholder: '搜索用户或视频...',
  autoSearch: true
})

const router = useRouter()
const searchBarRef = ref<HTMLElement | null>(null)
const keyword = ref('')
const showResults = ref(false)
const users = ref<UserSummaryVO[]>([])
const videos = ref<VideoListVO[]>([])
const loading = ref(false)

// 搜索建议（联想词）
const performSuggestion = async () => {
  if (!keyword.value.trim()) {
    users.value = []
    videos.value = []
    showResults.value = false
    return
  }

  loading.value = true
  try {
    // 获取少量结果作为联想
    const [usersResult, videosResult] = await Promise.all([
      searchApi.searchUsers(keyword.value, 1, 3), 
      searchApi.searchVideos(keyword.value, 1, 5) 
    ])

    users.value = usersResult
    videos.value = videosResult
    showResults.value = true
  } catch (error) {
    console.error('获取联想词失败:', error)
  } finally {
    loading.value = false
  }
}

// 防抖获取联想
const debouncedSuggestion = debounce(performSuggestion, 300)

// 处理输入
const handleInput = () => {
  if (props.autoSearch) {
    debouncedSuggestion()
  }
}

// 执行完整搜索并跳转
const handleSearch = () => {
  if (!keyword.value.trim()) return
  showResults.value = false
  router.push({
    path: '/search',
    query: { q: keyword.value }
  })
}

// 选择联想词
const selectSuggestion = (text: string) => {
  keyword.value = text
  handleSearch() // 选择后直接搜索
}

// 处理聚焦
const handleFocus = () => {
  if (keyword.value.trim() && (users.value.length > 0 || videos.value.length > 0)) {
    showResults.value = true
  }
}

// 点击外部关闭结果
const handleClickOutside = (event: MouseEvent) => {
  if (searchBarRef.value && !searchBarRef.value.contains(event.target as Node)) {
    showResults.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>

.search-bar {
  position: relative;
  width: 100%;
  max-width: 600px;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 24px;
  padding: 8px 16px;
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.search-input-wrapper:focus-within {
  background: #000; /* 背景保持黑色 */
  border-color: #fff; /* 边框变白 */
  box-shadow: 0 0 0 1px #fff; /* 增强白色边框效果 */
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 14px;
  outline: none;
  color: var(--text); /* 使用全局文字颜色 */
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.search-btn {
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  transition: color 0.3s ease;
}

.search-btn:hover {
  color: #fff;
}

.search-results {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: var(--bg-card, #252525);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  max-height: 480px;
  overflow-y: auto;
  z-index: 1000;
  padding: 8px 0;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* 自定义滚动条 */
.search-results::-webkit-scrollbar {
  width: 6px;
}

.search-results::-webkit-scrollbar-track {
  background: transparent;
}

.search-results::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.group-list + .group-list {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  margin-top: 4px;
  padding-top: 4px;
}

.group-title {
  padding: 4px 16px;
  font-size: 12px;
   color: rgba(255, 255, 255, 0.5);
}

.suggestion-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  transition: background 0.2s ease;
  color: var(--text);
  font-size: 14px;
}

.suggestion-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.suggestion-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}
</style>

