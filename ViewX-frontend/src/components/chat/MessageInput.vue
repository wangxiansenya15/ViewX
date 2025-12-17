<template>
  <div class="message-input">
    <div class="input-area">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="3"
        :maxlength="maxLength"
        show-word-limit
        placeholder="输入消息... (Ctrl+Enter 发送)"
        @keydown.ctrl.enter="handleSend"
        @input="handleTyping"
      />
    </div>

    <div class="input-actions">
      <div class="tips">
        <el-icon><InfoFilled /></el-icon>
        <span v-if="!isMutualFollow" class="warning">
          非互关用户只能发送一条不超过100字的消息
        </span>
        <span v-else class="normal">
          互关好友，畅聊无限
        </span>
      </div>

      <el-button
        type="primary"
        :disabled="!canSend"
        @click="handleSend"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useChatStore } from '@/stores/chat'

const emit = defineEmits<{
  send: [content: string]
  typing: []
}>()

const chatStore = useChatStore()
const inputText = ref('')
const typingTimeout = ref<number>()

// 是否互相关注（这里简化处理，实际应该从会话信息中获取）
const isMutualFollow = computed(() => {
  // TODO: 从会话信息中获取互关状态
  return true
})

// 最大字数限制
const maxLength = computed(() => {
  return isMutualFollow.value ? 1000 : 100
})

// 是否可以发送
const canSend = computed(() => {
  const text = inputText.value.trim()
  return text.length > 0 && text.length <= maxLength.value
})

// 处理输入
function handleTyping() {
  // 清除之前的定时器
  if (typingTimeout.value) {
    clearTimeout(typingTimeout.value)
  }

  // 发送正在输入状态
  emit('typing')

  // 3秒后清除状态
  typingTimeout.value = window.setTimeout(() => {
    // 状态已在 store 中自动清除
  }, 3000)
}

// 发送消息
function handleSend() {
  const text = inputText.value.trim()
  
  if (!text) {
    return
  }

  if (text.length > maxLength.value) {
    ElMessage.warning(`消息长度不能超过 ${maxLength.value} 字`)
    return
  }

  emit('send', text)
  inputText.value = ''
}
</script>

<style scoped>
.message-input {
  border-top: 1px solid #e4e7ed;
  background: white;
  padding: 16px 24px;
}

.input-area {
  margin-bottom: 12px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tips {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}

.tips .warning {
  color: #e6a23c;
}

.tips .normal {
  color: #67c23a;
}

:deep(.el-textarea__inner) {
  resize: none;
  border-radius: 8px;
}

:deep(.el-textarea .el-input__count) {
  background: transparent;
}
</style>
