<template>
  <div class="h-full w-full bg-[var(--bg)]">
    <!-- 移动端 -->
    <div v-if="isMobile" class="h-full flex flex-col">
      <!-- 头部 -->
      <div class="flex items-center justify-between px-4 py-3 border-b border-[var(--border)] bg-[var(--bg-glass)] backdrop-blur-xl sticky top-0 z-10">
        <button @click="$router.back()" class="p-2 -ml-2 text-[var(--text)] active:scale-90 transition-transform">
          <ArrowLeft class="w-5 h-5" />
        </button>
        <h1 class="text-lg font-bold text-[var(--text)]">通知</h1>
        <button 
          v-if="unreadCount > 0"
          @click="markAllAsRead" 
          class="text-sm text-indigo-500 active:scale-95 transition-transform"
        >
          全部已读
        </button>
        <div v-else class="w-16"></div>
      </div>

      <!-- 通知列表 -->
      <div class="flex-1 overflow-y-auto">
        <div v-if="loading" class="flex items-center justify-center py-12">
          <div class="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin"></div>
        </div>

        <div v-else-if="notifications.length === 0" class="flex flex-col items-center justify-center py-20 text-gray-500">
          <Bell class="w-16 h-16 mb-4 opacity-30" />
          <p class="text-sm">暂无通知</p>
        </div>

        <div v-else>
          <div
            v-for="notification in notifications"
            :key="notification.id"
            @click="handleNotificationClick(notification)"
            class="px-4 py-4 border-b border-[var(--border)] active:bg-white/5 transition-colors"
            :class="{ 'bg-indigo-500/5': !notification.isRead }"
          >
            <div class="flex gap-3">
              <!-- 头像 -->
              <img 
                v-if="notification.senderAvatar"
                :src="notification.senderAvatar" 
                class="w-12 h-12 rounded-full flex-shrink-0 cursor-pointer hover:ring-2 hover:ring-indigo-500 transition-all"
                alt="avatar"
                @click.stop="goToUserProfile(notification.senderId)"
              />
              <div 
                v-else 
                class="w-12 h-12 rounded-full bg-gradient-to-br from-indigo-500 to-purple-500 flex items-center justify-center flex-shrink-0 cursor-pointer hover:ring-2 hover:ring-indigo-500 transition-all"
                @click.stop="goToUserProfile(notification.senderId)"
              >
                <Bell class="w-6 h-6 text-white" />
              </div>

              <!-- 内容 -->
              <div class="flex-1 min-w-0">
                <div class="flex items-start justify-between gap-2 mb-1">
                  <p class="text-sm text-[var(--text)] font-medium">
                    <span 
                      v-if="notification.senderNickname" 
                      class="cursor-pointer hover:text-indigo-500 transition-colors"
                      @click.stop="goToUserProfile(notification.senderId)"
                    >
                      {{ notification.senderNickname }}
                    </span>
                    <span class="text-[var(--muted)] ml-1">{{ notification.notificationTypeDesc }}</span>
                  </p>
                  <div v-if="!notification.isRead" class="w-2 h-2 bg-indigo-500 rounded-full flex-shrink-0 mt-1"></div>
                </div>
                
                <p v-if="notification.relatedVideoTitle" class="text-xs text-[var(--muted)] truncate mb-1">
                  {{ notification.relatedVideoTitle }}
                </p>
                
                <p v-if="notification.content" class="text-xs text-[var(--muted)] line-clamp-2 mb-1">
                  {{ notification.content }}
                </p>
                
                <p class="text-xs text-[var(--muted)]">{{ notification.timeDesc }}</p>
              </div>

              <!-- 视频封面 -->
              <img 
                v-if="notification.relatedVideoCover"
                :src="notification.relatedVideoCover" 
                class="w-16 h-16 rounded-lg object-cover flex-shrink-0"
                alt="cover"
              />
            </div>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMore && !loading" class="py-4 text-center">
          <button 
            @click="loadMore"
            class="text-sm text-indigo-500 active:scale-95 transition-transform"
          >
            加载更多
          </button>
        </div>
      </div>
    </div>

    <!-- PC端 -->
    <div v-else class="max-w-4xl mx-auto py-8 px-6">
      <!-- 头部 -->
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-[var(--text)]">通知中心</h1>
        <div class="flex items-center gap-4">
          <!-- 筛选 -->
          <select 
            v-model="filterType"
            @change="fetchNotifications(true)"
            class="px-3 py-2 bg-[var(--bg)] border border-[var(--border)] rounded-lg text-sm text-[var(--text)] focus:outline-none focus:border-indigo-500"
          >
            <option value="">全部通知</option>
            <option value="FOLLOW">关注</option>
            <option value="LIKE_VIDEO">点赞</option>
            <option value="FAVORITE_VIDEO">收藏</option>
            <option value="COMMENT_VIDEO">评论</option>
          </select>

          <button 
            v-if="unreadCount > 0"
            @click="markAllAsRead"
            class="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm rounded-lg transition-colors"
          >
            全部已读
          </button>
        </div>
      </div>

      <!-- 通知列表 -->
      <div class="bg-[var(--bg-glass)] backdrop-blur-xl border border-[var(--border)] rounded-xl overflow-hidden">
        <div v-if="loading" class="flex items-center justify-center py-20">
          <div class="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin"></div>
        </div>

        <div v-else-if="notifications.length === 0" class="flex flex-col items-center justify-center py-20 text-gray-500">
          <Bell class="w-20 h-20 mb-4 opacity-30" />
          <p>暂无通知</p>
        </div>

        <div v-else>
          <div
            v-for="notification in notifications"
            :key="notification.id"
            @click="handleNotificationClick(notification)"
            class="px-6 py-4 hover:bg-white/5 cursor-pointer transition-colors border-b border-[var(--border)] last:border-b-0"
            :class="{ 'bg-indigo-500/5': !notification.isRead }"
          >
            <div class="flex gap-4">
              <!-- 头像 -->
              <img 
                v-if="notification.senderAvatar"
                :src="notification.senderAvatar" 
                class="w-14 h-14 rounded-full flex-shrink-0 cursor-pointer hover:ring-2 hover:ring-indigo-500 transition-all"
                alt="avatar"
                @click.stop="goToUserProfile(notification.senderId)"
              />
              <div 
                v-else 
                class="w-14 h-14 rounded-full bg-gradient-to-br from-indigo-500 to-purple-500 flex items-center justify-center flex-shrink-0 cursor-pointer hover:ring-2 hover:ring-indigo-500 transition-all"
                @click.stop="goToUserProfile(notification.senderId)"
              >
                <Bell class="w-7 h-7 text-white" />
              </div>

              <!-- 内容 -->
              <div class="flex-1 min-w-0">
                <div class="flex items-start justify-between gap-4 mb-2">
                  <div>
                    <p class="text-base text-[var(--text)] font-medium mb-1">
                      <span 
                        v-if="notification.senderNickname" 
                        class="cursor-pointer hover:text-indigo-500 transition-colors"
                        @click.stop="goToUserProfile(notification.senderId)"
                      >
                        {{ notification.senderNickname }}
                      </span>
                      <span class="text-[var(--muted)] ml-2">{{ notification.notificationTypeDesc }}</span>
                    </p>
                    
                    <p v-if="notification.relatedVideoTitle" class="text-sm text-[var(--muted)] mb-1">
                      {{ notification.relatedVideoTitle }}
                    </p>
                    
                    <p v-if="notification.content" class="text-sm text-[var(--muted)] line-clamp-2 mb-2">
                      {{ notification.content }}
                    </p>
                    
                    <p class="text-xs text-[var(--muted)]">{{ notification.timeDesc }}</p>
                  </div>

                  <div class="flex items-center gap-3 flex-shrink-0">
                    <!-- 视频封面 -->
                    <img 
                      v-if="notification.relatedVideoCover"
                      :src="notification.relatedVideoCover" 
                      class="w-24 h-24 rounded-lg object-cover"
                      alt="cover"
                    />
                    
                    <!-- 未读标记 -->
                    <div v-if="!notification.isRead" class="w-2 h-2 bg-indigo-500 rounded-full"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMore && !loading && notifications.length > 0" class="py-6 text-center border-t border-[var(--border)]">
          <button 
            @click="loadMore"
            class="px-6 py-2 text-sm text-indigo-500 hover:text-indigo-400 transition-colors"
          >
            加载更多
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, inject, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, ArrowLeft } from 'lucide-vue-next'
import { notificationApi, type NotificationVO, type NotificationType } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const isMobile = inject<Ref<boolean>>('isMobile', ref(false))

const loading = ref(false)
const notifications = ref<NotificationVO[]>([])
const unreadCount = ref(0)
const page = ref(1)
const pageSize = 20
const hasMore = ref(true)
const filterType = ref<NotificationType | ''>('')

// 获取通知列表
const fetchNotifications = async (reset = false) => {
  if (reset) {
    page.value = 1
    notifications.value = []
    hasMore.value = true
  }

  loading.value = true
  try {
    const list = await notificationApi.getNotifications({
      notificationType: filterType.value || undefined,
      page: page.value,
      pageSize
    })
    
    if (reset) {
      notifications.value = list
    } else {
      notifications.value.push(...list)
    }
    
    hasMore.value = list.length === pageSize
  } catch (e) {
    console.error('Failed to fetch notifications', e)
    ElMessage.error('获取通知失败')
  } finally {
    loading.value = false
  }
}

// 获取未读数量
const fetchUnreadCount = async () => {
  try {
    unreadCount.value = await notificationApi.getUnreadCount()
  } catch (e) {
    console.error('Failed to fetch unread count', e)
  }
}

// 加载更多
const loadMore = () => {
  page.value++
  fetchNotifications()
}

// 标记所有为已读
const markAllAsRead = async () => {
  try {
    await notificationApi.markAllAsRead()
    notifications.value.forEach(n => n.isRead = true)
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch (e) {
    console.error('Failed to mark all as read', e)
    ElMessage.error('操作失败')
  }
}

// 处理通知点击
const handleNotificationClick = async (notification: NotificationVO) => {
  // 标记为已读
  if (!notification.isRead) {
    try {
      await notificationApi.markAsRead(notification.id)
      notification.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (e) {
      console.error('Failed to mark as read', e)
    }
  }

  // 跳转到相关页面
  if (notification.relatedVideoId) {
    router.push(`/video/${notification.relatedVideoId}`)
  } else if (notification.senderId) {
    router.push(`/profile/${notification.senderId}`)
  }
}

// 跳转到用户主页
const goToUserProfile = (userId?: number) => {
  if (!userId) return
  router.push(`/profile/${userId}`)
}

onMounted(() => {
  fetchNotifications()
  fetchUnreadCount()
})
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
