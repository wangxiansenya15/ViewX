<template>
  <div class="mobile-chat-page">
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="30"><Loading /></el-icon>
    </div>
    
    <ChatWindow 
      v-else-if="chatStore.currentConversation"
      :conversation="chatStore.currentConversation"
      :messages="chatStore.currentMessages"
      :is-typing="chatStore.isTyping"
      :loading="chatStore.loading"
      class="mobile-chat-window"
      :show-back="true"
      @send="handleSendMessage"
      @typing="handleTyping"
      @close="goBack" 
    />
    
    <div v-else class="error-state">
      <p>无法加载会话</p>
      <el-button @click="goBack">返回</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useChatStore, useUserStore } from '@/stores'
import ChatWindow from '@/components/chat/ChatWindow.vue'
import { Loading } from '@element-plus/icons-vue'
import { userApi, type ConversationVO } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()
const userStore = useUserStore()
const loading = ref(true)

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

function goBack() {
  chatStore.clearCurrentConversation()
  router.back()
}

// 初始化会话
async function initConversation() {
  const userId = route.params.userId as string
  if (!userId) {
    goBack()
    return
  }
  
  if (!userStore.isLoggedIn) {
     router.replace('/login')
     return
  }
  
  loading.value = true

  try {
      // 确保 WebSocket 已连接
      await chatStore.initWebSocket()
      
      // 确保会话列表已加载
      if (chatStore.conversations.length === 0) {
          await chatStore.loadConversations()
      }

      // 查找或创建会话
      const userIdStr = userId.toString()
      const existingConversation = chatStore.conversations.find(
        conv => conv.otherUserId.toString() === userIdStr
      )
      
      if (existingConversation) {
        chatStore.selectConversation(existingConversation)
      } else {
        const userProfile = await userApi.getUserProfile(userIdStr)
        const tempConversation: ConversationVO = {
            conversationId: 0,
            otherUserId: userIdStr,
            otherUserUsername: userProfile.username,
            otherUserNickname: userProfile.nickname,
            otherUserAvatar: userProfile.avatarUrl,
            isOnline: false,
            lastMessage: '',
            lastMessageType: 'TEXT',
            lastMessageTime: new Date().toISOString(),
            unreadCount: 0
        }
        chatStore.selectConversation(tempConversation)
      }
  } catch (error) {
      console.error('Failed to init conversation:', error)
      ElMessage.error('无法打开对话')
  } finally {
      loading.value = false
  }
}

onMounted(() => {
  initConversation()
})

onUnmounted(() => {
  chatStore.clearCurrentConversation()
})
</script>

<style scoped>
.mobile-chat-page {
  height: 100vh;
  width: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.mobile-chat-window {
  flex: 1;
  height: 100%;
}

.loading-state, .error-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: #909399;
}

/* Override ChatWindow styles for mobile if needed */
:deep(.chat-header) {
  padding: 12px 16px;
}

:deep(.close-btn) {
  order: -1; 
  margin-right: auto;
  margin-left: 0;
  padding: 0;
  height: auto;
}

/* Adjust header layout to put back button on left, user info in center/left */
:deep(.chat-header) {
  justify-content: flex-start;
  gap: 12px;
}

:deep(.user-info) {
  flex: 1;
}

/* Dark mode support */
@media (prefers-color-scheme: dark) {
  .mobile-chat-page {
    background: #000;
  }
}
</style>
