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
          @close="handleCloseConversation"
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
import { onMounted, onUnmounted, watch, inject, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useChatStore, useUserStore } from '@/stores'
import ChatList from '@/components/chat/ChatList.vue'
import ChatWindow from '@/components/chat/ChatWindow.vue'
import { Loading, ChatDotRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { userApi, type ConversationVO } from '@/api'


const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()
const userStore = useUserStore()
const isMobile = inject<Ref<boolean>>('isMobile')!

// 选择会话
function handleSelectConversation(conversation: ConversationVO) {
  if (isMobile.value) {
      router.push(`/chat/${conversation.otherUserId}`)
  } else {
      chatStore.selectConversation(conversation)
  }
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

// 关闭会话
function handleCloseConversation() {
  chatStore.clearCurrentConversation()
}

// 根据 userId 查询参数打开对话
async function openConversationByUserId(userId: string | number) {
  if (!userId) return

  if (isMobile.value) {
    router.replace(`/chat/${userId}`)
    return
  }
  
  // Keep as string to avoid precision loss with large numbers (snowflake IDs)
  const userIdStr = typeof userId === 'number' ? userId.toString() : userId
  
  // 查找现有会话 - 需要转换为数字进行比较
  const existingConversation = chatStore.conversations.find(
    conv => conv.otherUserId.toString() === userIdStr
  )
  
  if (existingConversation) {
    // 如果会话已存在，直接选择
    chatStore.selectConversation(existingConversation)
    console.log('Selected existing conversation with user:', userIdStr)
  } else {
    // 如果会话不存在，获取用户信息并创建临时会话
    try {
      const userProfile = await userApi.getUserProfile(userIdStr)
      
      // 创建临时会话对象
      const tempConversation: ConversationVO = {
        conversationId: 0, // 临时ID，发送第一条消息时会创建真实会话
        otherUserId: userIdStr, // ✅ 直接使用字符串，避免大整数精度丢失
        otherUserUsername: userProfile.username,
        otherUserNickname: userProfile.nickname,
        otherUserAvatar: userProfile.avatarUrl,
        isOnline: false,
        lastMessage: '',
        lastMessageType: 'TEXT',
        lastMessageTime: new Date().toISOString(),
        unreadCount: 0
      }
      
      // 选择临时会话
      chatStore.selectConversation(tempConversation)
      console.log('Created temporary conversation with user:', userIdStr)
    } catch (error) {
      console.error('Failed to create conversation:', error)
      ElMessage.error('无法打开对话，请稍后重试')
    }
  }
}

// 初始化
onMounted(async () => {
  if (userStore.isLoggedIn) {
    await chatStore.initWebSocket()
    await chatStore.loadConversations()
    
    // 检查是否有 userId 查询参数
    if (route.query.userId) {
      await openConversationByUserId(route.query.userId as string)
    }
  }
})

// 监听路由变化
watch(() => route.query.userId, (newUserId) => {
  if (newUserId && chatStore.conversations.length > 0) {
    openConversationByUserId(newUserId as string)
  }
})

// 清理
onUnmounted(() => {
  chatStore.clearCurrentConversation()
})
</script>

<style scoped>
.messages-page {
  height: 100%;
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

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .messages-page {
    background: #121212; /* 极深背景 */
  }

  .messages-container {
    background: #1a1a1a; /* 容器背景 */
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
  }

  .conversations-panel {
    background: #1a1a1a;
    border-right-color: #333;
  }

  .panel-header {
    background: #1a1a1a;
    border-bottom-color: #333;
  }

  .panel-header h2 {
    color: #e5eaf3;
  }

  .chat-panel {
    background: #121212;
  }
  
  .empty-state {
    color: #606266;
  }
  
  .empty-state p {
    color: #909399;
  }
}
</style>
