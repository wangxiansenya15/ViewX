<template>
  <div class="messages-page">
    <div class="messages-container">
      <!-- 会话列表 -->
      <div class="conversations-panel">
        <div class="panel-header">
          <h2>消息</h2>
          <span v-if="chatStore.totalUnreadCount > 0" class="unread-badge">
            {{ chatStore.totalUnreadCount }}
          </span>
        </div>
        
        <div v-if="chatStore.loading" class="loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        
        <ChatList
          v-else
          :conversations="chatStore.conversations"
          :current-conversation="chatStore.currentConversation"
          @select="handleSelectConversation"
        />
      </div>

      <!-- 聊天窗口 -->
      <div class="chat-panel">
        <ChatWindow
          v-if="chatStore.currentConversation"
          :conversation="chatStore.currentConversation"
          :messages="chatStore.currentMessages"
          :is-typing="chatStore.isTyping"
          :loading="chatStore.loading"
          @send="handleSendMessage"
          @typing="handleTyping"
        />
        
        <div v-else class="empty-state">
          <el-icon :size="80" color="#ccc"><ChatDotRound /></el-icon>
          <p>选择一个会话开始聊天</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useChatStore, useUserStore } from '@/stores'
import ChatList from '@/components/chat/ChatList.vue'
import ChatWindow from '@/components/chat/ChatWindow.vue'
import { Loading, ChatDotRound } from '@element-plus/icons-vue'
import type { ConversationVO } from '@/api'

const chatStore = useChatStore()
const userStore = useUserStore()

// 选择会话
function handleSelectConversation(conversation: ConversationVO) {
  chatStore.selectConversation(conversation)
}

// 发送消息
async function handleSendMessage(content: string) {
  if (chatStore.currentConversation) {
    await chatStore.sendMessage(chatStore.currentConversation.otherUserId, content)
  }
}

// 发送正在输入状态
function handleTyping() {
  if (chatStore.currentConversation) {
    chatStore.sendTyping(chatStore.currentConversation.otherUserId)
  }
}

// 初始化
onMounted(async () => {
  if (userStore.isLoggedIn) {
    await chatStore.initWebSocket()
    await chatStore.loadConversations()
  }
})

// 清理
onUnmounted(() => {
  chatStore.clearCurrentConversation()
})
</script>

<style scoped>
.messages-page {
  height: 100vh;
  background: #f5f5f5;
  overflow: hidden;
}

.messages-container {
  display: flex;
  height: 100%;
  max-width: 1400px;
  margin: 0 auto;
  background: white;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.conversations-panel {
  width: 360px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: white;
}

.panel-header {
  padding: 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.unread-badge {
  background: #f56c6c;
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #909399;
  gap: 12px;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  gap: 16px;
}

.empty-state p {
  font-size: 16px;
  margin: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .conversations-panel {
    width: 100%;
    border-right: none;
  }

  .chat-panel {
    display: none;
  }

  .messages-container.has-conversation .conversations-panel {
    display: none;
  }

  .messages-container.has-conversation .chat-panel {
    display: flex;
  }
}
</style>
