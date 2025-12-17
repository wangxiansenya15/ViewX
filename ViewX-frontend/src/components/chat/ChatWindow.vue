<template>
  <div class="chat-window">
    <!-- 头部 -->
    <div class="chat-header">
      <div class="user-info">
        <el-avatar :size="40" :src="conversation.otherUserAvatar">
          {{ conversation.otherUserNickname[0] }}
        </el-avatar>
        <div class="info">
          <span class="nickname">{{ conversation.otherUserNickname }}</span>
          <span class="status">
            <span v-if="conversation.isOnline" class="online">在线</span>
            <span v-else class="offline">离线</span>
          </span>
        </div>
      </div>
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
          <span>{{ conversation.otherUserNickname }} 正在输入...</span>
        </div>
      </div>
    </div>

    <!-- 输入框 -->
    <MessageInput
      @send="handleSend"
      @typing="$emit('typing')"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import type { ConversationVO, MessageVO } from '@/api'
import MessageItem from './MessageItem.vue'
import MessageInput from './MessageInput.vue'

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
}>()

const messagesContainer = ref<HTMLElement>()

// 滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 发送消息
function handleSend(content: string) {
  emit('send', content)
  scrollToBottom()
}

// 监听消息变化，自动滚动
watch(() => props.messages, () => {
  scrollToBottom()
}, { deep: true })

// 监听会话变化，滚动到底部
watch(() => props.conversation, () => {
  scrollToBottom()
})
</script>

<style scoped>
.chat-window {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: white;
}

.chat-header {
  padding: 16px 24px;
  border-bottom: 1px solid #e4e7ed;
  background: white;
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
  color: #303133;
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
  overflow-y: auto;
  background: #f8f9fa;
  padding: 20px;
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
}

.typing-indicator {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  color: #909399;
  font-size: 13px;
  font-style: italic;
}
</style>
