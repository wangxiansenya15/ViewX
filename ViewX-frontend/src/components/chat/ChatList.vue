<template>
  <div class="chat-list">
    <div
      v-for="conversation in conversations"
      :key="conversation.conversationId"
      class="conversation-item"
      :class="{ active: isActive(conversation) }"
      @click="$emit('select', conversation)"
    >
      <!-- 头像 -->
      <div class="avatar-wrapper">
        <el-avatar :size="48" :src="conversation.otherUserAvatar">
          {{ conversation.otherUserNickname[0] }}
        </el-avatar>
        <span v-if="conversation.isOnline" class="online-dot"></span>
      </div>

      <!-- 信息 -->
      <div class="conversation-info">
        <div class="top-row">
          <span class="nickname">{{ conversation.otherUserNickname }}</span>
          <span class="time">{{ formatTime(conversation.lastMessageTime) }}</span>
        </div>
        <div class="bottom-row">
          <span class="last-message">{{ conversation.lastMessage }}</span>
          <el-badge
            v-if="conversation.unreadCount > 0"
            :value="conversation.unreadCount"
            :max="99"
            class="unread-count"
          />
        </div>
      </div>
    </div>

    <div v-if="conversations.length === 0" class="empty">
      <el-icon :size="60" color="#ccc"><ChatLineRound /></el-icon>
      <p>暂无消息</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ChatLineRound } from '@element-plus/icons-vue'
import type { ConversationVO } from '@/api'
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'

interface Props {
  conversations: ConversationVO[]
  currentConversation: ConversationVO | null
}

const props = defineProps<Props>()
defineEmits<{
  select: [conversation: ConversationVO]
}>()

function isActive(conversation: ConversationVO) {
  return props.currentConversation?.conversationId === conversation.conversationId
}

function formatTime(time: string) {
  try {
    return formatDistanceToNow(new Date(time), {
      addSuffix: true,
      locale: zhCN
    })
  } catch {
    return ''
  }
}
</script>

<style scoped>
.chat-list {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid #f0f0f0;
}

.conversation-item:hover {
  background-color: #f5f7fa;
}

.conversation-item.active {
  background-color: #ecf5ff;
}

.avatar-wrapper {
  position: relative;
  margin-right: 12px;
  flex-shrink: 0;
}

.online-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  background: #67c23a;
  border: 2px solid white;
  border-radius: 50%;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.nickname {
  font-weight: 600;
  color: #303133;
  font-size: 15px;
}

.time {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  margin-left: 8px;
}

.bottom-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.last-message {
  flex: 1;
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-count {
  flex-shrink: 0;
  margin-left: 8px;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #909399;
  gap: 12px;
}

.empty p {
  margin: 0;
  font-size: 14px;
}
/* ... (existing styles) ... */

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .conversation-item {
    border-bottom-color: #333;
  }
  
  .conversation-item:hover {
    background-color: #2c2c2c;
  }
  
  .conversation-item.active {
    background-color: #363637;
  }
  
  .nickname {
    color: #e5eaf3;
  }
  
  .last-message {
    color: #a3a6ad;
  }
  
  .time {
    color: #73767a;
  }
  
  .online-dot {
    border-color: #1a1a1a; /* 融入深色背景 */
  }
  
  .empty {
    color: #606266;
  }
}
</style>
