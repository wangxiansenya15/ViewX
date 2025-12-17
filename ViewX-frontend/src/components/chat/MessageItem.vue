<template>
  <div class="message-item" :class="{ 'is-mine': isMine }">
    <!-- 头像 -->
    <el-avatar
      v-if="!isMine"
      :size="36"
      :src="message.senderAvatar"
      class="avatar"
    >
      {{ message.senderNickname[0] }}
    </el-avatar>

    <!-- 消息内容 -->
    <div class="message-content">
      <div v-if="!isMine" class="sender-name">{{ message.senderNickname }}</div>
      
      <div class="message-bubble" :class="{ mine: isMine }">
        <div class="text">{{ message.content }}</div>
      </div>

      <div class="message-time">
        {{ formatTime(message.createdAt) }}
        <span v-if="isMine && message.isRead" class="read-status">已读</span>
      </div>
    </div>

    <!-- 我的头像 -->
    <el-avatar
      v-if="isMine"
      :size="36"
      :src="message.senderAvatar"
      class="avatar"
    >
      {{ message.senderNickname[0] }}
    </el-avatar>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores'
import type { MessageVO } from '@/api'
import { format } from 'date-fns'

interface Props {
  message: MessageVO
}

const props = defineProps<Props>()
const userStore = useUserStore()

// 是否是我发送的消息
const isMine = computed(() => {
  return props.message.senderId === userStore.userInfo?.id
})

// 格式化时间
function formatTime(time: string) {
  try {
    return format(new Date(time), 'HH:mm')
  } catch {
    return ''
  }
}
</script>

<style scoped>
.message-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.message-item.is-mine {
  flex-direction: row-reverse;
}

.avatar {
  flex-shrink: 0;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 60%;
}

.is-mine .message-content {
  align-items: flex-end;
}

.sender-name {
  font-size: 12px;
  color: #909399;
  padding: 0 12px;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  background: white;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  word-wrap: break-word;
  word-break: break-word;
}

.message-bubble.mine {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.text {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message-time {
  font-size: 11px;
  color: #909399;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.read-status {
  color: #67c23a;
}

/* 动画 */
.message-item {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
