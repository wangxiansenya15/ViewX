<template>
  <div 
    class="message-item" 
    :class="{ 'is-mine': isMine, 'is-recalled': message.isRecalled }"
    @contextmenu.prevent="handleContextMenu"
  >
    <!-- 头像 -->
    <el-avatar
      v-if="!isMine"
      :size="36"
      :src="displayAvatar"
      class="avatar"
    >
      {{ displayNickname[0] }}
    </el-avatar>

    <!-- 消息内容 -->
    <div class="message-content">
      <div v-if="!isMine" class="sender-name">{{ displayNickname }}</div>
      
      <!-- 已撤回的消息 -->
      <div v-if="message.isRecalled" class="message-bubble recalled">
        <div class="text">
          <el-icon><WarningFilled /></el-icon>
          消息已撤回
        </div>
      </div>
      
      <!-- 正常消息 -->
      <div v-else class="message-bubble" :class="{ mine: isMine }">
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
      :src="displayAvatar"
      class="avatar"
    >
      {{ displayNickname[0] }}
    </el-avatar>

    <!-- 右键菜单 -->
    <teleport to="body">
      <div
        v-if="showContextMenu"
        class="context-menu"
        :style="{ top: menuPosition.y + 'px', left: menuPosition.x + 'px' }"
        @click="closeContextMenu"
      >
        <div
          v-if="canRecall"
          class="menu-item"
          @click="handleRecall"
        >
          <el-icon><RefreshLeft /></el-icon>
          <span>撤回消息</span>
        </div>
        <div
          class="menu-item"
          @click="handleDelete"
        >
          <el-icon><Delete /></el-icon>
          <span>删除消息</span>
        </div>
        <div
          class="menu-item"
          @click="handleCopy"
        >
          <el-icon><DocumentCopy /></el-icon>
          <span>复制内容</span>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useUserStore, useChatStore } from '@/stores'
import { chatApi, type MessageVO } from '@/api'
import { format } from 'date-fns'
import { ElMessage, ElMessageBox } from 'element-plus'
import { WarningFilled, RefreshLeft, Delete, DocumentCopy } from '@element-plus/icons-vue'

interface Props {
  message: MessageVO
}

const props = defineProps<Props>()
const userStore = useUserStore()
const chatStore = useChatStore()

// 右键菜单状态
const showContextMenu = ref(false)
const menuPosition = ref({ x: 0, y: 0 })

// 是否是我发送的消息（确保类型一致进行比较）
const isMine = computed(() => {
  if (!userStore.userInfo) return false
  // 将两边都转换为字符串进行比较，避免 number vs string 的问题
  return props.message.senderId.toString() === userStore.userInfo.id.toString()
})

// 是否可以撤回（2分钟内且是自己的消息）
const canRecall = computed(() => {
  if (!isMine.value || props.message.isRecalled) return false
  
  const now = new Date()
  const createdAt = new Date(props.message.createdAt)
  const diffMinutes = (now.getTime() - createdAt.getTime()) / 1000 / 60
  
  return diffMinutes <= 2
})

// 智能获取显示头像（优先使用消息自带，没有则使用本地缓存）
const displayAvatar = computed(() => {
  if (props.message.senderAvatar) return props.message.senderAvatar
  
  if (isMine.value) {
    return userStore.userInfo?.avatar
  } else {
    // 尝试从当前会话缓存中获取对方头像
    if (chatStore.currentConversation && 
        chatStore.currentConversation.otherUserId.toString() === props.message.senderId.toString()) {
      return chatStore.currentConversation.otherUserAvatar
    }
  }
  return ''
})

// 智能获取显示昵称
const displayNickname = computed(() => {
  if (props.message.senderNickname) return props.message.senderNickname
  
  if (isMine.value) {
    return userStore.userInfo?.nickname || userStore.userInfo?.username || '我'
  } else {
    if (chatStore.currentConversation && 
        chatStore.currentConversation.otherUserId.toString() === props.message.senderId.toString()) {
      return chatStore.currentConversation.otherUserNickname
    }
  }
  return '未知用户'
})

// 格式化时间
function formatTime(time: string) {
  try {
    return format(new Date(time), 'HH:mm')
  } catch {
    return ''
  }
}

// 处理右键菜单
function handleContextMenu(event: MouseEvent) {
  // 已撤回的消息不显示菜单
  if (props.message.isRecalled) return
  
  menuPosition.value = {
    x: event.clientX,
    y: event.clientY
  }
  showContextMenu.value = true
}

// 关闭右键菜单
function closeContextMenu() {
  showContextMenu.value = false
}

// 撤回消息
async function handleRecall() {
  closeContextMenu()
  
  try {
    await ElMessageBox.confirm(
      '撤回后，对方将无法查看此消息',
      '确认撤回消息？',
      {
        confirmButtonText: '撤回',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await chatApi.recallMessage(props.message.id)
    ElMessage.success('消息已撤回')
    
    // 更新本地消息状态
    props.message.isRecalled = true
    props.message.recalledAt = new Date().toISOString()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '撤回失败')
    }
  }
}

// 删除消息
async function handleDelete() {
  closeContextMenu()
  
  try {
    await ElMessageBox.confirm(
      '删除后，此消息将从聊天记录中移除',
      '确认删除消息？',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await chatApi.deleteMessage(props.message.id)
    ElMessage.success('消息已删除')
    
    // TODO: 从消息列表中移除此消息
    // 这需要通过 emit 通知父组件
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

// 复制内容
async function handleCopy() {
  closeContextMenu()
  
  try {
    await navigator.clipboard.writeText(props.message.content)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

// 点击其他地方关闭菜单
function handleClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (!target.closest('.context-menu')) {
    closeContextMenu()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.message-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  position: relative;
}

.message-item.is-mine {
  /* 移除 flex-direction: row-reverse，因为 DOM 顺序已经是 [内容, 头像] */
  justify-content: flex-end;
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
  background: var(--el-bg-color-overlay, white);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  word-wrap: break-word;
  word-break: break-word;
  cursor: context-menu;
  transition: all 0.2s;
}

.message-bubble:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.message-bubble.mine {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white; /* 自己的消息保持白色文字 */
}

/* 撤回的消息样式 */
.message-bubble.recalled {
  background: #f5f7fa;
  color: #909399;
  font-style: italic;
  cursor: default;
}

.message-bubble.recalled .text {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
}

.text {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  color: var(--el-text-color-primary, #303133);
}

.message-bubble.mine .text {
  color: white; /* 覆盖上面的颜色 */
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

/* 右键菜单样式 */
.context-menu {
  position: fixed;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 4px 0;
  min-width: 140px;
  z-index: 9999;
  animation: menuFadeIn 0.15s ease;
}

@keyframes menuFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.2s;
  font-size: 14px;
  color: #303133;
}

.menu-item:hover {
  background: #f5f7fa;
}

.menu-item .el-icon {
  font-size: 16px;
  color: #606266;
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

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .message-bubble:not(.mine):not(.recalled) {
    background: #2c2c2c;
    box-shadow: none;
  }
  
  .message-bubble:not(.mine):not(.recalled) .text {
    color: #e5eaf3;
  }
  
  .message-bubble.recalled {
    background: #2c2c2c;
    color: #73767a;
  }
  
  .message-bubble.recalled .text {
    color: #73767a;
  }
  
  .sender-name {
    color: #a3a6ad;
  }
  
  .context-menu {
    background: #2c2c2c;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  }
  
  .menu-item {
    color: #e5eaf3;
  }
  
  .menu-item:hover {
    background: #363637;
  }
  
  .menu-item .el-icon {
    color: #a3a6ad;
  }
}
</style>
