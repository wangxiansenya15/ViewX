<template>
  <transition name="fade">
    <div v-if="video" class="fixed inset-0 z-50 flex bg-[#050505] animate-in fade-in duration-300">

      <!-- 返回按钮 -->
      <div class="absolute top-6 left-6 z-50">
        <button @click="$emit('close')"
          class="p-3 rounded-full bg-white/10 hover:bg-white/20 backdrop-blur-md border border-white/10 text-white spring-transition click-spring group">
          <ArrowLeft class="w-6 h-6 group-hover:-translate-x-1 transition-transform" />
        </button>
      </div>

      <!-- 内容容器 -->
      <div class="flex flex-col lg:flex-row w-full h-full">

        <!-- 视频播放区 (左侧/上方) -->
        <div class="flex-1 relative bg-black flex items-center justify-center group overflow-hidden">
          <!-- 环境光背景 -->
          <div class="absolute inset-0 opacity-30 blur-3xl scale-125 transition-all duration-1000"
            :style="`background-image: url(${video.cover}); background-size: cover; background-position: center;`">
          </div>

          <!-- 模拟播放器 -->
          <div class="relative w-full max-w-5xl aspect-video bg-black shadow-2xl overflow-hidden group/player">
            <img :src="video.cover" class="w-full h-full object-contain opacity-90">

            <!-- 弹幕层 -->
            <div class="absolute inset-0 overflow-hidden z-10 pointer-events-none mask-linear-gradient">
              <div v-for="dm in activeDanmakuList" :key="dm.id"
                class="danmaku-item text-white/90 text-lg lg:text-2xl font-bold drop-shadow-md"
                :style="{ top: dm.top + '%', animationDuration: dm.speed + 's', fontSize: dm.size + 'px' }">
                {{ dm.text }}
              </div>
            </div>

            <!-- 播放器控制栏 (Hover显示) -->
            <div
              class="absolute bottom-0 left-0 right-0 p-4 bg-gradient-to-t from-black/80 to-transparent opacity-0 group-hover/player:opacity-100 transition-opacity duration-300">
              <!-- 进度条 -->
              <div
                class="w-full h-1 bg-white/20 rounded-full mb-4 cursor-pointer hover:h-1.5 transition-all relative group/progress">
                <div class="w-1/3 h-full bg-indigo-500 rounded-full relative">
                  <div
                    class="absolute right-0 top-1/2 -translate-y-1/2 w-3 h-3 bg-white rounded-full shadow scale-0 group-hover/progress:scale-100 transition-transform">
                  </div>
                </div>
              </div>
              <!-- 按钮组 -->
              <div class="flex justify-between items-center">
                <div class="flex gap-4">
                  <Play class="w-6 h-6 fill-white" />
                  <Volume2 class="w-6 h-6" />
                  <span class="text-sm font-medium">04:20 / {{ video.duration }}</span>
                </div>
                <div class="flex gap-4">
                  <span class="text-sm font-bold border border-white/30 px-2 rounded">4K</span>
                  <Maximize class="w-6 h-6" />
                </div>
              </div>
            </div>

            <!-- 中心播放按钮 -->
            <div class="absolute inset-0 flex items-center justify-center pointer-events-none">
              <button
                class="w-20 h-20 rounded-full bg-white/10 backdrop-blur-sm flex items-center justify-center border border-white/20 opacity-0 group-hover/player:opacity-100 transition-all scale-90 group-hover/player:scale-100">
                <Play class="w-8 h-8 fill-white ml-1" />
              </button>
            </div>
          </div>
        </div>

        <!-- 侧边互动区 (右侧/下方) -->
        <div
          class="w-full lg:w-[400px] h-1/2 lg:h-full glass-panel border-l border-white/10 border-t-0 flex flex-col z-20">

          <!-- UP主信息 -->
          <div class="p-6 border-b border-white/5 flex items-center justify-between bg-white/5">
            <div class="flex items-center gap-3">
              <img :src="video.avatar" class="w-10 h-10 rounded-full border border-indigo-500/30">
              <div>
                <h3 class="font-bold text-sm text-gray-100">{{ video.author }}</h3>
                <p class="text-xs text-gray-400 mt-0.5">粉丝 58.2万</p>
              </div>
            </div>
            <button
              class="px-5 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold rounded-full spring-transition click-spring shadow-lg shadow-indigo-600/20">
              关注
            </button>
          </div>

          <!-- 视频标题与数据 + AI 入口 -->
          <div class="px-6 py-4 border-b border-white/5 space-y-3">
            <h2 class="text-lg font-bold leading-snug text-white">{{ video.title }}</h2>
            <div class="flex items-center gap-4 text-gray-400 text-xs">
              <div class="flex items-center gap-1 hover:text-red-400 cursor-pointer transition-colors">
                <Flame class="w-4 h-4" /> 2.4万
              </div>
              <div class="flex items-center gap-1 hover:text-blue-400 cursor-pointer transition-colors">
                <MessageCircle class="w-4 h-4" /> 1,024
              </div>
              <span class="ml-auto text-gray-600">{{ video.date }}</span>
            </div>

            <!-- AI 视频洞察区域 -->
            <div class="mt-2">
              <button v-if="!aiSummary && !isGeneratingAI" @click="generateVideoInsight"
                class="w-full flex items-center justify-center gap-2 py-2 rounded-xl bg-gradient-to-r from-indigo-500/10 to-purple-500/10 border border-indigo-500/20 text-indigo-300 text-xs font-bold hover:bg-white/5 transition-all group">
                <Sparkles class="w-3.5 h-3.5 group-hover:rotate-12 transition-transform" />
                ✨ 生成 AI 视频洞察
              </button>

              <div v-if="isGeneratingAI"
                class="w-full p-4 rounded-xl ai-card animate-pulse flex items-center justify-center gap-2 text-xs text-indigo-200">
                <Loader2 class="w-3.5 h-3.5 animate-spin" />
                Gemini 正在分析视频内容...
              </div>

              <div v-if="aiSummary" class="p-4 rounded-xl ai-card space-y-2 animate-in fade-in slide-in-from-bottom-2">
                <div class="flex items-center gap-2 text-indigo-300 text-xs font-bold uppercase tracking-wider mb-1">
                  <Bot class="w-3.5 h-3.5" />
                  Gemini Insight
                </div>
                <div class="text-xs text-gray-200 leading-relaxed space-y-2" v-html="renderMarkdown(aiSummary)"></div>
              </div>
            </div>
          </div>

          <!-- 评论/弹幕列表 -->
          <div class="flex-1 overflow-y-auto p-0 relative bg-black/20" ref="commentList">
            <!-- 评论列表 -->
            <div class="p-4 space-y-5">
              <div v-for="(comment, index) in comments" :key="index" class="flex gap-3 animate-slide-up"
                :style="{ animationDelay: index * 50 + 'ms' }">
                <img :src="`https://api.dicebear.com/7.x/notionists/svg?seed=${index + 5}`"
                  class="w-8 h-8 rounded-full bg-gray-700 mt-1">
                <div class="flex-1">
                  <div class="flex justify-between items-baseline">
                    <span class="text-xs font-bold text-gray-400">用户_{{ 1000 + index }}</span>
                    <span class="text-[10px] text-gray-600">2小时前</span>
                  </div>
                  <p class="text-sm text-gray-200 mt-1 leading-relaxed">{{ comment }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 底部输入栏 (带 AI 辅助) -->
          <div class="p-4 bg-black/40 backdrop-blur-lg border-t border-white/5">
            <div class="relative w-full">
              <input v-model="inputDanmaku" @keyup.enter="send" type="text"
                :placeholder="isMagicLoading ? 'Gemini 正在思考有趣的弹幕...' : '发个弹幕见证当下...'" :disabled="isMagicLoading"
                class="w-full bg-white/5 border border-white/10 rounded-full py-3 pl-10 pr-12 text-sm text-white focus:bg-white/10 focus:border-indigo-500/50 focus:outline-none transition-all placeholder-gray-500 disabled:opacity-50">

              <!-- AI Magic Button -->
              <button @click="magicComment" :disabled="isMagicLoading" title="AI 帮你写评论"
                class="absolute left-1.5 top-1.5 p-1.5 rounded-full hover:bg-indigo-500/20 text-indigo-400 transition-colors group z-10">
                <component :is="isMagicLoading ? Loader2 : Sparkles"
                  :class="['w-4 h-4', isMagicLoading ? 'animate-spin' : '']" />
              </button>

              <button @click="send"
                class="absolute right-2 top-2 p-1.5 bg-indigo-600 rounded-full hover:bg-indigo-500 text-white transition-colors click-spring">
                <Send class="w-4 h-4 ml-0.5" />
              </button>
            </div>
          </div>

        </div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { ArrowLeft, Play, Volume2, Maximize, Flame, MessageCircle, Sparkles, Loader2, Bot, Send } from 'lucide-vue-next'
import { marked } from 'marked'

const props = defineProps<{
  video: any
}>()

const emit = defineEmits(['close'])

// State
const inputDanmaku = ref('')
const activeDanmakuList = ref<any[]>([])
const aiSummary = ref("")
const isGeneratingAI = ref(false)
const isMagicLoading = ref(false)
const comments = ref([
  "这个UI设计太绝了，求开源！",
  "up主高产似母猪",
  "第一视角既视感太强了",
  "背景音乐请问是什么？",
  "这就去买食材做起来",
  "这就是未来的交互方式吗",
  "火钳刘明",
  "不仅视频质量高，网页做得也太好看了"
])

const defaultDanmaku = ["前方高能", "好听到怀孕", "见证历史", "太强了", "又在骗我买装备", "泪目", "这个转场丝滑", "233333"]
let danmakuInterval: any = null

// Methods
const startDanmakuEngine = () => {
  if (danmakuInterval) clearInterval(danmakuInterval)
  danmakuInterval = setInterval(() => {
    if (Math.random() > 0.4) {
      const text = defaultDanmaku[Math.floor(Math.random() * defaultDanmaku.length)]
      addDanmaku(text)
    }
  }, 800)
}

const stopDanmakuEngine = () => {
  if (danmakuInterval) clearInterval(danmakuInterval)
  activeDanmakuList.value = []
}

const addDanmaku = (text: string, isSelf = false) => {
  const id = Date.now() + Math.random()
  const dm = {
    id,
    text,
    top: Math.random() * 80, // 0-80% top
    speed: isSelf ? 8 : 6 + Math.random() * 6, // 速度
    size: isSelf ? 24 : 16 + Math.random() * 8 // 字体大小
  }
  activeDanmakuList.value.push(dm)

  // 动画结束后清理（简单处理）
  setTimeout(() => {
    activeDanmakuList.value = activeDanmakuList.value.filter(d => d.id !== id)
  }, 15000)
}

const send = () => {
  if (!inputDanmaku.value.trim()) return
  // 发送弹幕
  addDanmaku(inputDanmaku.value, true)
  // 添加评论
  comments.value.unshift(inputDanmaku.value)
  inputDanmaku.value = ''
}

// AI Functions (Mocked for now as per original code structure, but using fetch if key exists)
const apiKey = "" // Inject key if available

async function callGemini(prompt: string) {
  if (!apiKey) {
    // Mock response if no key
    return new Promise(resolve => setTimeout(() => resolve("AI Key 未配置，这是模拟的回复。"), 1000))
  }
  try {
    const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-09-2025:generateContent?key=${apiKey}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [{ parts: [{ text: prompt }] }]
      })
    })
    const data = await response.json()
    return data.candidates?.[0]?.content?.parts?.[0]?.text || "AI 暂时休息了，请稍后再试。"
  } catch (error) {
    console.error("Gemini API Error:", error)
    return "网络连接异常。"
  }
}

const generateVideoInsight = async () => {
  if (!props.video || isGeneratingAI.value) return
  isGeneratingAI.value = true

  const prompt = `你是 ViewX 视频平台的 AI 助手。请根据视频标题'${props.video.title}'和作者'${props.video.author}'，生成一段简短精彩的视频内容简介（50字以内）以及三个看点（bullet points）。请用中文回答，语气轻松吸引人，并适当使用 emoji。`

  aiSummary.value = await callGemini(prompt) as string
  isGeneratingAI.value = false
}

const magicComment = async () => {
  if (!props.video || isMagicLoading.value) return
  isMagicLoading.value = true

  const prompt = `请为标题为'${props.video.title}'的视频写一条简短有趣的中文评论或弹幕（30字以内），带一个emoji，风格要像真实的互联网用户。只返回评论内容本身。`

  const result = await callGemini(prompt) as string
  inputDanmaku.value = result.replace(/^["']|["']$/g, '')
  isMagicLoading.value = false
}

const renderMarkdown = (text: string) => {
  return marked.parse(text)
}

// Lifecycle
onMounted(() => {
  startDanmakuEngine()
})

onUnmounted(() => {
  stopDanmakuEngine()
})

watch(() => props.video, (newVal) => {
  if (newVal) {
    aiSummary.value = ""
    activeDanmakuList.value = []
    startDanmakuEngine()
  } else {
    stopDanmakuEngine()
  }
})
</script>
