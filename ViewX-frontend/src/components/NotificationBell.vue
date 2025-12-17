<template>
  <div class="relative">
    <!-- 通知铃铛按钮 -->
    <button 
      @click="toggleDropdown"
      class="relative p-2 text-[var(--muted)] hover:text-[var(--text)] transition-colors active:scale-90 click-spring"
      :class="{ 'text-indigo-500': hasUnread }"
    >
      <Bell class="w-5 h-5" />
      <!-- 未读数量徽章 -->
      <span 
        v-if="unreadCount > 0" 
        class="absolute top-1.5 right-1.5 min-w-[16px] h-4 px-1 bg-red-500 rounded-full text-[10px] font-bold text-white flex items-center justify-center border border-[var(--bg)] shadow-sm"
      >
        {{ unreadCount > 99 ? '99+' : unreadCount }}
      </span>
    </button>

    <!-- 通知下拉框 -->
    <transition name="dropdown">
      <div 
        v-if="showDropdown" 
        class="absolute right-0 mt-2 w-80 max-h-96 bg-[var(--bg-glass)] backdrop-blur-xl border border-[var(--border)] rounded-xl shadow-2xl overflow-hidden z-50"
        @click.stop
      >
        <!-- 头部 -->
        <div class="flex items-center justify-between px-4 py-3 border-b border-[var(--border)]">
          <h3 class="text-sm font-bold text-[var(--text)]">通知</h3>
          <button 
            v-if="unreadCount > 0"
            @click="markAllAsRead"
            class="text-xs text-indigo-500 hover:text-indigo-400 transition-colors"
          >
            全部已读
          </button>
        </div>

        <!-- 通知列表 -->
        <div class="overflow-y-auto max-h-80 custom-scrollbar">
          <div v-if="loading" class="flex items-center justify-center py-8">
            <div class="w-6 h-6 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin"></div>
          </div>

          <div v-else-if="notifications.length === 0" class="flex flex-col items-center justify-center py-8 text-gray-500">
            <Bell class="w-12 h-12 mb-2 opacity-30" />
            <p class="text-sm">暂无通知</p>
          </div>

          <div v-else>
            <div
              v-for="notification in notifications"
              :key="notification.id"
              @click="handleNotificationClick(notification)"
              class="px-4 py-3 hover:bg-white/5 cursor-pointer transition-colors border-b border-[var(--border)] last:border-b-0"
              :class="{ 'bg-indigo-500/5': !notification.isRead }"
            >
              <div class="flex gap-3">
                <!-- 发送者头像 -->
                <img 
                  v-if="notification.senderAvatar"
                  :src="notification.senderAvatar" 
                  class="w-10 h-10 rounded-full flex-shrink-0 cursor-pointer hover:ring-2 hover:ring-indigo-500 transition-all"
                  alt="avatar"
                  @click.stop="goToUserProfile(notification.senderId)"
                />
                <div 
                  v-else 
                  class="w-10 h-10 rounded-full bg-gradient-to-br from-indigo-500 to-purple-500 flex items-center justify-center flex-shrink-0 cursor-pointer hover:ring-2 hover:ring-indigo-500 transition-all"
                  @click.stop="goToUserProfile(notification.senderId)"
                >
                  <Bell class="w-5 h-5 text-white" />
                </div>

                <!-- 通知内容 -->
                <div class="flex-1 min-w-0">
                  <p class="text-sm text-[var(--text)] mb-1">
                    <span 
                      v-if="notification.senderNickname" 
                      class="font-medium cursor-pointer hover:text-indigo-500 transition-colors"
                      @click.stop="goToUserProfile(notification.senderId)"
                    >
                      {{ notification.senderNickname }}
                    </span>
                    <span class="text-[var(--muted)] ml-1">{{ notification.notificationTypeDesc }}</span>
                  </p>
                  <p v-if="notification.relatedVideoTitle" class="text-xs text-[var(--muted)] truncate">
                    {{ notification.relatedVideoTitle }}
                  </p>
                  <p class="text-xs text-[var(--muted)] mt-1">{{ notification.timeDesc }}</p>
                </div>

                <!-- 未读标记 -->
                <div v-if="!notification.isRead" class="w-2 h-2 bg-indigo-500 rounded-full flex-shrink-0 mt-2"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部 -->
        <div class="px-4 py-3 border-t border-[var(--border)] text-center">
          <router-link 
            to="/notifications" 
            class="text-sm text-indigo-500 hover:text-indigo-400 transition-colors"
            @click="showDropdown = false"
          >
            查看全部通知
          </router-link>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Bell } from 'lucide-vue-next'
import { notificationApi, type NotificationVO } from '@/api'
import { useRouter } from 'vue-router'

const router = useRouter()

const showDropdown = ref(false)
const loading = ref(false)
const unreadCount = ref(0)
const notifications = ref<NotificationVO[]>([])

let refreshInterval: any = null

// 切换下拉框
const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
  if (showDropdown.value) {
    fetchNotifications()
  }
}

// 获取未读数量
const fetchUnreadCount = async () => {
  try {
    const count = await notificationApi.getUnreadCount()
    unreadCount.value = count
  } catch (e) {
    console.error('Failed to fetch unread count', e)
  }
}

// 获取通知列表
const fetchNotifications = async () => {
  loading.value = true
  try {
    const list = await notificationApi.getNotifications({ page: 1, pageSize: 10 })
    notifications.value = list
  } catch (e) {
    console.error('Failed to fetch notifications', e)
  } finally {
    loading.value = false
  }
}

// 标记所有为已读
const markAllAsRead = async () => {
  try {
    await notificationApi.markAllAsRead()
    notifications.value.forEach(n => n.isRead = true)
    unreadCount.value = 0
  } catch (e) {
    console.error('Failed to mark all as read', e)
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
  showDropdown.value = false
  if (notification.relatedVideoId) {
    router.push(`/video/${notification.relatedVideoId}`)
  } else if (notification.senderId) {
    router.push(`/profile/${notification.senderId}`)
  }
}

// 跳转到用户主页
const goToUserProfile = (userId?: number) => {
  if (!userId) return
  showDropdown.value = false
  router.push(`/profile/${userId}`)
}


// 点击外部关闭下拉框
const handleClickOutside = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (!target.closest('.relative')) {
    showDropdown.value = false
  }
}

const hasUnread = computed(() => unreadCount.value > 0)

onMounted(() => {
  fetchUnreadCount()
  // 每30秒刷新一次未读数量
  refreshInterval = setInterval(fetchUnreadCount, 30000)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
  document.removeEventListener('click', handleClickOutside)
})
</script>

<script lang="ts">
import { computed } from 'vue'
export default {
  name: 'NotificationBell'
}
</script>

<style scoped>
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.2);
}
</style>
