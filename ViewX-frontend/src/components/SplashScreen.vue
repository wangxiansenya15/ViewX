<template>
  <Transition name="splash">
    <div v-if="show" class="fixed inset-0 z-[9999] flex flex-col items-center justify-center bg-[var(--bg)]">
      <div class="flex flex-col items-center gap-8">
        <!-- Logo Animation -->
        <div class="relative w-24 h-24 animate-float">
           <div class="absolute inset-0 bg-gradient-to-tr from-indigo-500 to-purple-500 rounded-3xl rotate-6 opacity-50 blur-lg animate-pulse"></div>
           <div class="relative w-full h-full bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 rounded-3xl flex items-center justify-center shadow-2xl z-10 logo-spring">
             <Play class="w-10 h-10 text-white fill-white" />
           </div>
        </div>
        
        <div class="flex flex-col items-center gap-2">
          <h1 class="text-4xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-indigo-500 to-purple-600 tracking-tight">ViewX</h1>
          <p class="text-[var(--muted)] text-sm tracking-widest uppercase opacity-80">{{ t('splash.slogan') }}</p>
        </div>
        
        <!-- Progress Bar -->
        <div class="w-48 h-1 bg-[var(--border)] rounded-full overflow-hidden mt-8 relative">
          <div class="absolute inset-0 bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 w-full h-full origin-left transition-transform duration-300 ease-out-back"
               :style="{ transform: `scaleX(${progress / 100})` }"></div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { Play } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

defineProps<{
  show: boolean
  progress: number
}>()
</script>

<style scoped>
.splash-leave-active {
  transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.splash-leave-to {
  opacity: 0;
  transform: scale(1.1);
  filter: blur(10px);
}

.animate-float {
  animation: float 3s ease-in-out infinite;
}

.logo-spring {
  transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.ease-out-back {
  transition-timing-function: cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
</style>
