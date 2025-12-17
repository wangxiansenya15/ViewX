<template>
  <button 
    class="follow-btn"
    :class="buttonClass"
    @click="handleFollow"
    :disabled="loading || isSelf"
  >
    <span class="btn-icon" v-if="!loading">
      <svg v-if="!followStatus.isFollowing" width="16" height="16" viewBox="0 0 16 16" fill="none">
        <path d="M8 2v12M2 8h12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
      <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="none">
        <path d="M13 4L6 11l-3-3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </span>
    <span class="btn-text">{{ buttonText }}</span>
  </button>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { interactionApi, type FollowStatusVO } from '@/api'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores'


const props = defineProps<{
  userId: number
  size?: 'small' | 'medium' | 'large'
}>()

const emit = defineEmits<{
  statusChange: [status: FollowStatusVO]
}>()

const userStore = useUserStore()
const loading = ref(false)
const followStatus = ref<FollowStatusVO>({
  isFollowing: false,
  isFollower: false,
  isMutual: false,
  statusText: '关注'
})

// 是否是自己
const isSelf = computed(() => {
  return userStore.userInfo?.id === props.userId
})

// 按钮文本
const buttonText = computed(() => {
  if (isSelf.value) return '自己'
  if (loading.value) return '处理中...'
  return followStatus.value.statusText
})

// 按钮样式类
const buttonClass = computed(() => {
  const classes = [`follow-btn--${props.size || 'medium'}`]
  
  if (isSelf.value) {
    classes.push('follow-btn--self')
  } else if (followStatus.value.isMutual) {
    classes.push('follow-btn--mutual')
  } else if (followStatus.value.isFollowing) {
    classes.push('follow-btn--following')
  } else {
    classes.push('follow-btn--default')
  }
  
  return classes.join(' ')
})

// 加载关注状态
const loadFollowStatus = async () => {
  if (!userStore.isLoggedIn || isSelf.value) return
  
  try {
    const status = await interactionApi.getDetailedFollowStatus(props.userId)
    followStatus.value = status
  } catch (error) {
    console.error('加载关注状态失败:', error)
  }
}

// 处理关注/取消关注
const handleFollow = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  
  if (isSelf.value) return
  
  loading.value = true
  try {
    const message = await interactionApi.toggleFollow(props.userId)
    ElMessage.success(message)
    
    // 重新加载状态
    await loadFollowStatus()
    emit('statusChange', followStatus.value)
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}

// 监听用户ID变化
watch(() => props.userId, () => {
  loadFollowStatus()
}, { immediate: true })

onMounted(() => {
  loadFollowStatus()
})

// 暴露方法供父组件调用
defineExpose({
  refresh: loadFollowStatus
})
</script>

<style scoped>
.follow-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 20px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: inherit;
  white-space: nowrap;
}

.follow-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

/* 尺寸变体 */
.follow-btn--small {
  padding: 4px 12px;
  font-size: 12px;
  gap: 4px;
}

.follow-btn--medium {
  padding: 8px 16px;
  font-size: 14px;
}

.follow-btn--large {
  padding: 12px 24px;
  font-size: 16px;
}

/* 状态变体 */
.follow-btn--default {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.follow-btn--default:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.follow-btn--following {
  background: #f0f0f0;
  color: #666;
  border: 1px solid #ddd;
}

.follow-btn--following:hover:not(:disabled) {
  background: #fee;
  color: #f56565;
  border-color: #f56565;
}

.follow-btn--following:hover:not(:disabled) .btn-text::after {
  content: ' (取消)';
  font-size: 0.9em;
}

.follow-btn--mutual {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.follow-btn--mutual:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 87, 108, 0.4);
}

.follow-btn--self {
  background: #e0e0e0;
  color: #999;
  cursor: not-allowed;
}

.btn-icon {
  display: flex;
  align-items: center;
  line-height: 1;
}

.btn-text {
  line-height: 1;
}
</style>
