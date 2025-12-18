<template>
  <div class="chat-window">
    <!-- 头部 -->
    <div class="chat-header">
      <div class="user-info">
        <el-avatar :size="40" :src="displayAvatar">
          {{ displayName[0]?.toUpperCase() }}
        </el-avatar>
        <div class="info">
          <span class="nickname">{{ displayName }}</span>
          <span class="status">
            <span v-if="conversation.isOnline" class="online">在线</span>
            <span v-else class="offline">离线</span>
          </span>
        </div>
      </div>
      
      <!-- 关闭按钮 -->
      <el-button 
        class="close-btn" 
        link 
        @click="$emit('close')"
        title="关闭会话"
      >
        <el-icon :size="20"><Close /></el-icon>
      </el-button>
    </div>

    <!-- 消息列表 -->
    <div ref="messagesContainer" class="messages-container">
      <div v-if="loading" class="loading">
        <el-icon class="is-loading"><Loading /></el-icon>
      </div>

      <div v-else class="messages-list">
        <MessageItem
          v-for="message in messages"
          :key="message.id"
          :message="message"
        />

        <!-- 正在输入提示 -->
        <div v-if="isTyping" class="typing-indicator">
          <span>{{ displayName }} 正在输入...</span>
        </div>
      </div>
    </div>

    <!-- 输入框 -->
    <div class="chat-footer">
      <MessageInput
        @send="handleSend"
        @typing="$emit('typing')"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, computed } from 'vue'
import { Loading, Close } from '@element-plus/icons-vue'
import type { ConversationVO, MessageVO } from '@/api'
import MessageItem from './MessageItem.vue'
import MessageInput from './MessageInput.vue'
import { useChatStore } from '@/stores'

interface Props {
  conversation: ConversationVO
  messages: MessageVO[]
  isTyping: boolean
  loading: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  send: [content: string]
  typing: []
  close: []
}>()

const chatStore = useChatStore()
const messagesContainer = ref<HTMLElement>()

// 本地暂存的用户资料（用于补全 conversation 中缺失的信息）
const localProfile = ref<{
  nickname?: string
  avatarUrl?: string
}>({})

// 获取最新的用户资料
async function fetchUserProfile() {
  if (!props.conversation.otherUserId) return
  
  try {
    const { userApi } = await import('@/api')
    // 能够处理 string 或 number 类型的 ID
    const profile = await userApi.getUserProfile(props.conversation.otherUserId)
    
    // 更新本地缓存
    localProfile.value = {
      nickname: profile.nickname,
      avatarUrl: profile.avatarUrl
    }
    
    // 同步更新全局 store 中的会话信息（这会即时修复左侧列表的显示）
    chatStore.updateConversationProfile(
      props.conversation.otherUserId,
      profile.nickname,
      profile.avatarUrl
    )
    
  } catch (error) {
    console.error('Failed to fetch user profile:', error)
  }
}

// 智能获取显示名称 (优先使用本地获取的最新昵称，其次是会话中的昵称，最后是用户名)
const displayName = computed(() => {
  return localProfile.value.nickname || 
         props.conversation.otherUserNickname || 
         props.conversation.otherUserUsername || 
         '用户'
})

// 智能获取显示头像
const displayAvatar = computed(() => {
  return localProfile.value.avatarUrl || 
         props.conversation.otherUserAvatar || 
         ''
})

// 滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 初始化检查
watch(() => props.conversation, () => {
  // 清空旧的本地数据
  localProfile.value = {}
  scrollToBottom()
  
  // 如果信息不完整（实际上为了保证最新，建议每次都查，或者至少在缺少头像/昵称时查）
  // 这里策略是：只要打开窗口，就尝试获取一次最新详情，确保昵称大小写和头像正确
  fetchUserProfile()
}, { immediate: true, deep: true })


// 发送消息
function handleSend(content: string) {
  emit('send', content)
  scrollToBottom()
}

// 监听消息变化，自动滚动
watch(() => props.messages, () => {
  scrollToBottom()
}, { deep: true })
</script>

<style scoped>
.chat-window {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--el-bg-color, white);
  overflow: hidden; /* 防止整体滚动 */
}

.chat-header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--el-border-color-lighter, #e4e7ed);
  background: var(--el-bg-color, white);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0; /* 防止被挤压或滚动 */
  z-index: 10; /* 确保在顶层 */
}

.chat-footer {
  flex-shrink: 0; /* 防止被挤压 */
  background: var(--el-bg-color, white);
  border-top: 1px solid var(--el-border-color-lighter, #e4e7ed);
  z-index: 10;
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .chat-footer {
    background: #1a1a1a;
    border-top-color: #363637;
  }
}

.close-btn {
  color: #909399;
  transition: color 0.3s;
}

.close-btn:hover {
  color: #f56c6c;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nickname {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary, #303133);
}

.status {
  font-size: 12px;
}

.online {
  color: #67c23a;
}

.offline {
  color: #909399;
}

.messages-container {
  flex: 1;
  overflow-y: auto; /* 仅此处滚动 */
  min-height: 0; /* Firefox/Flexbox 兼容性关键 */
  background: var(--el-bg-color-page, #f8f9fa);
  padding: 12px 16px;
}

.loading {
  display: flex;
  justify-content: center;
  padding: 40px;
  color: #909399;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 16px; /* 底部留白 */
}

.typing-indicator {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  color: #909399;
  font-size: 13px;
  font-style: italic;
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .chat-window {
    background: #1a1a1a;
  }
  
  .chat-header {
    background: #1a1a1a;
    border-bottom-color: #363637;
  }
  
  .nickname {
    color: #e5eaf3;
  }
  
  .close-btn {
    color: #606266;
  }
  
  .close-btn:hover {
    color: #f56c6c;
  }
  
  .messages-container {
    background: #0d0d0d;
  }
  
  .typing-indicator {
    color: #606266;
  }
}
</style>
