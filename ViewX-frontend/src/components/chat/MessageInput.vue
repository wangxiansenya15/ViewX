<template>
  <div class="message-input">
    <!-- 输入框区域 -->
    <div class="input-area">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="5"
        :maxlength="maxLength"
        placeholder="输入消息... (Enter 发送)"
        @keydown.enter.exact.prevent="handleSend"
        @input="handleTyping"
      />
    </div>

    <!-- 底部操作栏 -->
    <div class="footer">
      <div class="tips">
        <el-icon><InfoFilled /></el-icon>
        <span v-if="!isMutualFollow" class="warning">
          非互关用户只能发送一条不超过100字的消息
        </span>
        <span v-else class="normal">
          互关好友，畅聊无限
        </span>
      </div>

      <div class="actions">
        <!-- 字数统计 -->
        <span class="word-count">{{ inputText.length }}/{{ maxLength }}</span>
        
        <el-button
          type="primary"
          :disabled="!canSend"
          @click="handleSend"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const emit = defineEmits<{
  send: [content: string]
  typing: []
}>()

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
  border-top: 1px solid var(--el-border-color-lighter, #e4e7ed);
  background: var(--el-bg-color, white);
  padding: 16px 24px 24px;
  display: flex;
  flex-direction: column;
}

.input-area {
  flex: 1;
  margin-bottom: 12px;
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.word-count {
  font-size: 12px;
  color: #909399;
}

.tips {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}

.tips .warning { color: #e6a23c; }
.tips .normal { color: #67c23a; }

/* 输入框样式覆盖 */
:deep(.el-textarea__inner) {
  resize: none;
  border: none; /* 无边框 */
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  padding: 0;
  font-size: 14px;
  color: var(--el-text-color-primary, #303133);
}

:deep(.el-textarea__inner:focus) {
  box-shadow: none;
}

/* 隐藏 element-plus 自带的字数统计，因为我们自己实现了 */
:deep(.el-input__count) {
  display: none;
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .message-input {
    background: #1a1a1a; /* 深色背景 */
    border-top-color: #363637;
  }
  
  :deep(.el-textarea__inner) {
    color: #e5eaf3;
  }
  
  .word-count {
    color: #606266;
  }
}
</style>
