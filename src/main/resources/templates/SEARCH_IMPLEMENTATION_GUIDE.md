# 搜索功能完整实现指南

## ✅ 已完成

### 1. 数据库索引优化
- ✅ 创建了 `12_search_indexes.sql`
- ✅ 使用 `pg_trgm` 扩展支持高效模糊搜索
- ✅ 为用户名、昵称、视频标题创建了 GIN 索引

### 2. 后端 API

#### 用户搜索
- ✅ `GET /api/users/search?keyword=xxx&page=1&size=20`
- ✅ UserMapper.searchUsers() - 支持用户名和昵称搜索
- ✅ UserService.searchUsers() - 业务逻辑
- ✅ UserController.searchUsers() - API 端点

#### 视频搜索
- ✅ `GET /api/recommend/search?keyword=xxx&page=1&size=20`
- ⏳ 需要实现 RecommendService.searchVideos()

## 📝 待实现

### 后端：RecommendService.searchVideos()

在 `RecommendService.java` 接口中添加：

```java
/**
 * 搜索视频
 */
Result<List<VideoListVO>> searchVideos(String keyword, Long userId, int page, int size);
```

在 `RecommendServiceImpl.java` 中实现：

```java
@Override
public Result<List<VideoListVO>> searchVideos(String keyword, Long userId, int page, int size) {
    try {
        // 使用 MyBatis-Plus 的 QueryWrapper 进行搜索
        QueryWrapper<Video> query = new QueryWrapper<>();
        query.eq("is_deleted", false)
             .eq("status", "APPROVED")
             .and(wrapper -> wrapper
                 .like("title", keyword)
                 .or()
                 .like("description", keyword))
             .orderByDesc("created_at")
             .last("LIMIT " + size + " OFFSET " + ((page - 1) * size));
        
        List<Video> videos = videoMapper.selectList(query);
        
        // 转换为 VO 并填充额外信息
        List<VideoListVO> videoVOs = videos.stream()
            .map(video -> convertToVideoListVO(video, userId))
            .collect(Collectors.toList());
        
        log.info("搜索视频成功，关键词: {}, 结果数: {}", keyword, videoVOs.size());
        return Result.success(videoVOs);
    } catch (Exception e) {
        log.error("搜索视频失败，关键词: {}", keyword, e);
        return Result.serverError("搜索视频失败");
    }
}
```

### 前端：API 定义

在 `ViewX-frontend/src/api/index.ts` 中添加：

```typescript
// 搜索 API
export const search = {
    // 搜索用户
    searchUsers(keyword: string, page = 1, size = 20) {
        return request.get<UserSummaryVO[]>('/users/search', {
            params: { keyword, page, size }
        })
    },

    // 搜索视频
    searchVideos(keyword: string, page = 1, size = 20) {
        return request.get<VideoListVO[]>('/recommend/search', {
            params: { keyword, page, size }
        })
    }
}
```

### 前端：搜索组件

创建 `ViewX-frontend/src/components/common/SearchBar.vue`：

```vue
<template>
  <div class="search-bar">
    <div class="search-input-wrapper">
      <input
        v-model="keyword"
        type="text"
        class="search-input"
        :placeholder="placeholder"
        @keyup.enter="handleSearch"
        @input="handleInput"
      />
      <button class="search-btn" @click="handleSearch">
        <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
          <path d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"/>
        </svg>
      </button>
    </div>

    <!-- 搜索结果下拉 -->
    <div v-if="showResults && (users.length > 0 || videos.length > 0)" class="search-results">
      <!-- 用户结果 -->
      <div v-if="users.length > 0" class="result-section">
        <div class="section-title">用户</div>
        <div
          v-for="user in users"
          :key="user.id"
          class="result-item user-item"
          @click="goToUserProfile(user.id)"
        >
          <img :src="user.avatar || '/default-avatar.png'" class="user-avatar" />
          <div class="user-info">
            <div class="user-name">{{ user.nickname || user.username }}</div>
            <div class="user-username">@{{ user.username }}</div>
          </div>
        </div>
      </div>

      <!-- 视频结果 -->
      <div v-if="videos.length > 0" class="result-section">
        <div class="section-title">视频</div>
        <div
          v-for="video in videos"
          :key="video.id"
          class="result-item video-item"
          @click="goToVideo(video.id)"
        >
          <img :src="video.coverUrl" class="video-cover" />
          <div class="video-info">
            <div class="video-title">{{ video.title }}</div>
            <div class="video-meta">
              <span>{{ video.uploaderNickname }}</span>
              <span>·</span>
              <span>{{ video.viewCount }} 观看</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api as apiModule, type UserSummaryVO, type VideoListVO } from '@/api'
import { debounce } from 'lodash-es'

const api = apiModule

const props = defineProps<{
  placeholder?: string
  autoSearch?: boolean  // 是否自动搜索（输入时）
}>()

const router = useRouter()
const keyword = ref('')
const showResults = ref(false)
const users = ref<UserSummaryVO[]>([])
const videos = ref<VideoListVO[]>([])

// 搜索函数
const performSearch = async () => {
  if (!keyword.value.trim()) {
    users.value = []
    videos.value = []
    showResults.value = false
    return
  }

  try {
    // 并行搜索用户和视频
    const [usersResult, videosResult] = await Promise.all([
      api.search.searchUsers(keyword.value, 1, 5),
      api.search.searchVideos(keyword.value, 1, 5)
    ])

    users.value = usersResult
    videos.value = videosResult
    showResults.value = true
  } catch (error) {
    console.error('搜索失败:', error)
  }
}

// 防抖搜索
const debouncedSearch = debounce(performSearch, 300)

// 处理输入
const handleInput = () => {
  if (props.autoSearch) {
    debouncedSearch()
  }
}

// 处理搜索按钮点击
const handleSearch = () => {
  performSearch()
}

// 跳转到用户主页
const goToUserProfile = (userId: number) => {
  router.push(`/profile/${userId}`)
  showResults.value = false
  keyword.value = ''
}

// 跳转到视频详情
const goToVideo = (videoId: number) => {
  router.push(`/video/${videoId}`)
  showResults.value = false
  keyword.value = ''
}

// 点击外部关闭结果
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.search-bar')) {
    showResults.value = false
  }
}

// 监听点击事件
watch(() => showResults.value, (newVal) => {
  if (newVal) {
    document.addEventListener('click', handleClickOutside)
  } else {
    document.removeEventListener('click', handleClickOutside)
  }
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
  background: #f5f5f5;
  border-radius: 24px;
  padding: 8px 16px;
  transition: all 0.3s ease;
}

.search-input-wrapper:focus-within {
  background: white;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 14px;
  outline: none;
}

.search-btn {
  border: none;
  background: transparent;
  color: #666;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  transition: color 0.3s ease;
}

.search-btn:hover {
  color: #667eea;
}

.search-results {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  max-height: 400px;
  overflow-y: auto;
  z-index: 1000;
}

.result-section {
  padding: 12px 0;
}

.result-section + .result-section {
  border-top: 1px solid #eee;
}

.section-title {
  padding: 8px 16px;
  font-size: 12px;
  font-weight: 600;
  color: #999;
  text-transform: uppercase;
}

.result-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.result-item:hover {
  background: #f5f5f5;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 12px;
}

.user-info {
  flex: 1;
}

.user-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
}

.user-username {
  font-size: 12px;
  color: #999;
}

.video-cover {
  width: 80px;
  height: 45px;
  border-radius: 6px;
  object-fit: cover;
  margin-right: 12px;
}

.video-info {
  flex: 1;
}

.video-title {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.video-meta {
  font-size: 12px;
  color: #999;
  display: flex;
  gap: 6px;
}
</style>
```

### 使用示例

在主页中使用搜索组件：

```vue
<template>
  <div class="home-header">
    <SearchBar 
      placeholder="搜索用户或视频..." 
      :autoSearch="true"
    />
  </div>
</template>

<script setup>
import SearchBar from '@/components/common/SearchBar.vue'
</script>
```

## 🎯 功能特性

### 搜索优化
- ✅ 数据库索引优化（pg_trgm GIN 索引）
- ✅ 智能排序（精确匹配 > 前缀匹配 > 模糊匹配）
- ✅ 分页支持
- ✅ 防抖搜索（300ms）

### 用户体验
- ✅ 实时搜索建议
- ✅ 搜索结果预览
- ✅ 点击跳转到详情页
- ✅ 美观的 UI 设计

### 性能
- ✅ 并行搜索用户和视频
- ✅ 结果限制（默认各5条）
- ✅ 索引优化查询速度

## 📌 下一步

1. 实现 `RecommendService.searchVideos()`
2. 添加前端 API 定义
3. 创建搜索组件
4. 在主页集成搜索框
5. 测试搜索功能
