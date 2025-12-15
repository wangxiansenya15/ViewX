<template>
  <div class="h-full w-full bg-[var(--bg)] flex">
    <!-- 移动端 -->
    <div v-if="isMobile" class="h-full w-full flex flex-col">
      <!-- 头部 -->
      <div class="flex items-center justify-between px-4 py-3 border-b border-[var(--border)] bg-[var(--bg-glass)] backdrop-blur-xl sticky top-0 z-10">
        <button @click="$router.back()" class="p-2 -ml-2 text-[var(--text)] active:scale-90 transition-transform">
          <ArrowLeft class="w-5 h-5" />
        </button>
        <h1 class="text-lg font-bold text-[var(--text)]">消息</h1>
        <button class="p-2 -mr-2 text-[var(--text)] active:scale-90 transition-transform">
          <Plus class="w-5 h-5" />
        </button>
      </div>

      <!-- 消息列表 -->
      <div class="flex-1 overflow-y-auto">
        <div class="flex flex-col items-center justify-center py-20 text-gray-500">
          <MessageCircle class="w-16 h-16 mb-4 opacity-30" />
          <p class="text-sm mb-2">暂无消息</p>
          <p class="text-xs text-[var(--muted)]">私信功能即将上线</p>
        </div>
      </div>
    </div>

    <!-- PC端 -->
    <div v-else class="h-full w-full flex">
      <!-- 左侧:会话列表 -->
      <div class="w-80 border-r border-[var(--border)] flex flex-col bg-[var(--bg-glass)] backdrop-blur-xl">
        <!-- 头部 -->
        <div class="flex items-center justify-between px-4 py-4 border-b border-[var(--border)]">
          <h2 class="text-lg font-bold text-[var(--text)]">消息</h2>
          <button class="p-2 hover:bg-white/5 rounded-lg transition-colors">
            <Plus class="w-5 h-5 text-[var(--text)]" />
          </button>
        </div>

        <!-- 搜索 -->
        <div class="px-4 py-3">
          <div class="relative">
            <Search class="absolute left-3 top-2.5 w-4 h-4 text-[var(--muted)]" />
            <input 
              type="text" 
              placeholder="搜索消息..." 
              class="w-full pl-10 pr-4 py-2 bg-[var(--bg)] border border-[var(--border)] rounded-lg text-sm text-[var(--text)] placeholder-[var(--muted)] focus:outline-none focus:border-indigo-500"
            />
          </div>
        </div>

        <!-- 会话列表 -->
        <div class="flex-1 overflow-y-auto">
          <div class="flex flex-col items-center justify-center py-20 text-gray-500">
            <MessageCircle class="w-16 h-16 mb-4 opacity-30" />
            <p class="text-sm">暂无会话</p>
          </div>
        </div>
      </div>

      <!-- 右侧:聊天区域 -->
      <div class="flex-1 flex flex-col">
        <!-- 空状态 -->
        <div class="flex-1 flex flex-col items-center justify-center text-gray-500">
          <div class="w-20 h-20 rounded-full bg-gradient-to-br from-indigo-500 to-purple-500 flex items-center justify-center mb-4">
            <MessageCircle class="w-10 h-10 text-white" />
          </div>
          <h3 class="text-xl font-bold text-[var(--text)] mb-2">私信功能</h3>
          <p class="text-sm text-[var(--muted)] mb-6">与其他用户进行一对一聊天</p>
          <div class="flex flex-col gap-2 text-sm text-[var(--muted)]">
            <div class="flex items-center gap-2">
              <div class="w-1.5 h-1.5 bg-indigo-500 rounded-full"></div>
              <span>实时消息推送</span>
            </div>
            <div class="flex items-center gap-2">
              <div class="w-1.5 h-1.5 bg-indigo-500 rounded-full"></div>
              <span>支持图片、视频分享</span>
            </div>
            <div class="flex items-center gap-2">
              <div class="w-1.5 h-1.5 bg-indigo-500 rounded-full"></div>
              <span>消息已读状态</span>
            </div>
          </div>
          <div class="mt-8 px-6 py-3 bg-indigo-600/10 border border-indigo-500/30 rounded-lg">
            <p class="text-sm text-indigo-400">🚧 功能开发中,敬请期待...</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, inject, type Ref } from 'vue'
import { MessageCircle, ArrowLeft, Plus, Search } from 'lucide-vue-next'

const isMobile = inject<Ref<boolean>>('isMobile', ref(false))
</script>

<style scoped>
/* 自定义滚动条 */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.2);
}
</style>
