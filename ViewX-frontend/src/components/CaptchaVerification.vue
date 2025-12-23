<template>
  <div class="captcha-container">
    <!-- Cloudflare Turnstile 验证 -->
    <div v-if="type === 'turnstile'" class="captcha-wrapper">
      <!-- 直接渲染真实的 Cloudflare Widget，它本身就是截图中的白色卡片样式 -->
      <div ref="turnstileWidget" class="turnstile-widget"></div>
    </div>
    
    <!-- 滑块验证 (自定义) -->
    <div v-else-if="type === 'slider'" class="slider-captcha">
      <div class="slider-track">
        <div class="slider-fill" :style="{ width: sliderValue + '%' }"></div>
        <div 
          class="slider-thumb" 
          :style="{ left: sliderValue + '%' }"
          @mousedown="startDrag"
          @touchstart="startDrag"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>
      <div class="slider-text">{{ sliderText }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const props = defineProps({
  type: {
    type: String,
    default: 'turnstile',
    validator: (value) => ['turnstile', 'slider'].includes(value)
  },
  siteKey: {
    type: String,
    default: ''
  },
  theme: {
    type: String,
    default: 'dark',
  }
});

const emit = defineEmits(['verified', 'expired', 'error']);

// Cloudflare Turnstile
const turnstileWidget = ref(null);
let turnstileWidgetId = null;
const isVerified = ref(false);
const isLoading = ref(false);

// 触发验证逻辑（如果是自动模式Turnstile会自己处理，这里主要是视觉反馈）
const triggerVerify = () => {
  if (!isVerified.value && !isLoading.value) {
    // 实际的验证由隐藏的 Widget 处理，这里只是简单的交互反馈
    // 真实的 Turnstile 通常会自动执行或在点击 widget 时执行
  }
}

// 滑块验证变量
const sliderValue = ref(0);
const sliderText = ref('向右滑动完成验证');
const isDragging = ref(false);
const startX = ref(0);

// 初始化 Cloudflare Turnstile
const initTurnstile = () => {
  if (window.turnstile && turnstileWidget.value) {
    turnstileWidgetId = window.turnstile.render(turnstileWidget.value, {
      sitekey: props.siteKey,
      theme: 'light', // 强制使用白色主题
      // appearance: 'interaction-only', // 移除此配置，使用默认外观
      callback: (token) => {
        isLoading.value = false;
        isVerified.value = true;
        emit('verified', token);
      },
      'expired-callback': () => {
        isVerified.value = false;
        isLoading.value = false;
        emit('expired');
      },
      'error-callback': () => {
        isVerified.value = false;
        isLoading.value = false;
        emit('error');
      }
    });
  }
};

// ... 滑块逻辑保持不变 ...
const startDrag = (e) => {
  isDragging.value = true;
  startX.value = e.type === 'mousedown' ? e.clientX : e.touches[0].clientX;
  
  document.addEventListener('mousemove', onDrag);
  document.addEventListener('mouseup', stopDrag);
  document.addEventListener('touchmove', onDrag);
  document.addEventListener('touchend', stopDrag);
};

const onDrag = (e) => {
  if (!isDragging.value) return;
  
  const currentX = e.type === 'mousemove' ? e.clientX : e.touches[0].clientX;
  const container = e.target.closest('.slider-captcha');
  if (!container) return;
  
  const rect = container.querySelector('.slider-track').getBoundingClientRect();
  const deltaX = currentX - rect.left;
  const percentage = Math.max(0, Math.min(100, (deltaX / rect.width) * 100));
  
  sliderValue.value = percentage;
  
  if (percentage > 95) {
    sliderText.value = '验证成功!';
    setTimeout(() => {
      emit('verified', 'slider-verified');
    }, 300);
  }
};

const stopDrag = () => {
  isDragging.value = false;
  
  if (sliderValue.value < 95) {
    sliderValue.value = 0;
    sliderText.value = '验证失败,请重试';
    setTimeout(() => {
      sliderText.value = '向右滑动完成验证';
    }, 1000);
  }
  
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', stopDrag);
  document.removeEventListener('touchmove', onDrag);
  document.removeEventListener('touchend', stopDrag);
};

const loadScript = (src) => {
  return new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = src;
    script.async = true;
    script.defer = true;
    script.onload = resolve;
    script.onerror = reject;
    document.head.appendChild(script);
  });
};

onMounted(async () => {
  if (props.type === 'turnstile') {
    if (!window.turnstile) {
      await loadScript('https://challenges.cloudflare.com/turnstile/v0/api.js');
    }
    initTurnstile();
  }
});

onUnmounted(() => {
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', stopDrag);
  document.removeEventListener('touchmove', onDrag);
  document.removeEventListener('touchend', stopDrag);
});

const reset = () => {
  if (props.type === 'turnstile' && window.turnstile && turnstileWidgetId !== null) {
    window.turnstile.reset(turnstileWidgetId);
    isVerified.value = false;
    isLoading.value = false;
  } else if (props.type === 'slider') {
    sliderValue.value = 0;
    sliderText.value = '向右滑动完成验证';
  }
};

defineExpose({ reset });
</script>

<style scoped>
.captcha-container {
  width: 100%;
  padding: 8px 0;
}

.captcha-wrapper {
  width: 100%;
}

/* Turnstile 样式 */
.turnstile-widget {
  min-height: 65px; /* 预留高度防止抖动 */
  min-width: 300px;
  display: flex;
  justify-content: center;
}

/* 滑块验证样式 */

/* 滑块保持原样，略 */
.slider-captcha {
  width: 100%;
  max-width: 100%;
}
.slider-track {
  position: relative;
  width: 100%;
  height: 44px;
  background: rgba(255, 255, 255, 0.05); /* 适配深色背景 */
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}
.slider-fill {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: linear-gradient(90deg, #4f46e5, #06b6d4);
  border-radius: 12px;
  transition: width 0.1s ease;
  opacity: 0.6;
}
.slider-thumb {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  transition: all 0.2s ease;
  z-index: 10;
}
.slider-text {
  text-align: center;
  margin-top: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  user-select: none;
}
</style>
