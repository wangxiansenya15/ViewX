<template>
  <div class="h-full w-full overflow-y-auto bg-[#0f0f0f] pt-10 pb-20 px-4 sm:px-6 lg:px-8">
    <div class="max-w-4xl mx-auto">
      
      <div class="glass-panel rounded-3xl p-8 relative overflow-hidden">
        <!-- Background Decoration -->
        <div class="absolute top-0 right-0 -mr-20 -mt-20 w-96 h-96 bg-indigo-600/10 rounded-full blur-3xl"></div>
        <div class="absolute bottom-0 left-0 -ml-20 -mb-20 w-80 h-80 bg-purple-600/10 rounded-full blur-3xl"></div>
        
        <div class="relative z-10">
          <h1 class="text-2xl font-bold text-white mb-8 flex items-center">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 mr-3 text-indigo-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
            </svg>
            {{ t('upload.title') }}
          </h1>

          <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
            <!-- Left: Video Upload -->
            <div class="space-y-6">
              <div 
                class="border-2 border-dashed border-white/20 rounded-2xl h-64 flex flex-col items-center justify-center p-6 transition-colors"
                :class="{'border-indigo-500 bg-indigo-500/10': isDragging, 'hover:border-white/40 hover:bg-white/5': !videoFile}"
                @dragover.prevent="isDragging = true"
                @dragleave.prevent="isDragging = false"
                @drop.prevent="handleDrop"
                @click="triggerVideoInput"
              >
                <input type="file" ref="videoInput" class="hidden" accept="video/mp4,video/webm" @change="handleVideoSelect" />
                
                <template v-if="!videoFile">
                  <div class="w-16 h-16 rounded-full bg-white/10 flex items-center justify-center mb-4">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                    </svg>
                  </div>
                  <p class="text-white font-medium mb-1">{{ t('upload.dragDrop') }}</p>
                  <p class="text-gray-500 text-sm">{{ t('upload.support') }}</p>
                </template>

                <template v-else>
                   <div class="w-full h-full flex flex-col items-center justify-center">
                      <video class="w-full h-40 object-cover rounded-lg mb-2 bg-black" controls>
                        <source :src="videoPreviewUrl" type="video/mp4">
                      </video>
                      <div class="flex items-center gap-2 text-sm text-green-400">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                        </svg>
                        <span>{{ videoFile.name }}</span>
                      </div>
                      <button @click.stop="removeVideo" class="text-red-400 hover:text-red-300 text-xs mt-2 underline">移除</button>
                   </div>
                </template>
              </div>

              <!-- Cover Upload -->
              <div>
                <label class="block text-sm font-medium text-gray-400 mb-2">{{ t('upload.cover') }}</label>
                <div 
                  class="w-full h-40 bg-white/5 border border-white/10 rounded-xl flex items-center justify-center cursor-pointer hover:bg-white/10 transition-colors overflow-hidden group relative"
                  @click="triggerCoverInput"
                >
                  <img v-if="coverPreviewUrl" :src="coverPreviewUrl" class="w-full h-full object-cover" />
                  <div v-if="coverPreviewUrl" class="absolute inset-0 bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                     <span class="text-white text-sm">更换封面</span>
                  </div>
                  <div v-else class="flex flex-col items-center">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-gray-500 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                    <span class="text-gray-500 text-xs">{{ t('upload.uploadCover') }}</span>
                  </div>
                  <input type="file" ref="coverInput" class="hidden" accept="image/*" @change="handleCoverSelect" />
                </div>
              </div>
            </div>

            <!-- Right: Metadata Form -->
            <div class="space-y-6">
              <div>
                <label class="block text-sm font-medium text-gray-400 mb-2">{{ t('upload.videoTitle') }}</label>
                <input 
                  v-model="form.title" 
                  type="text" 
                  class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-600 focus:outline-none focus:border-indigo-500 focus:bg-white/10 transition-all"
                  placeholder="给视频起个引人注目的标题..."
                />
              </div>

              <div>
                <label class="block text-sm font-medium text-gray-400 mb-2">{{ t('upload.videoDesc') }}</label>
                <textarea 
                  v-model="form.description" 
                  rows="6" 
                  class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-600 focus:outline-none focus:border-indigo-500 focus:bg-white/10 transition-all resize-none"
                  placeholder="描述一下这个视频..."
                ></textarea>
              </div>

              <div class="pt-4">
                <button 
                  @click="handlePublish" 
                  :disabled="isPublishing || !isValid"
                  class="w-full py-3.5 bg-gradient-to-r from-indigo-600 to-purple-600 rounded-xl text-white font-bold shadow-lg shadow-indigo-500/20 hover:shadow-indigo-500/40 hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:translate-y-0 transition-all flex items-center justify-center gap-2"
                >
                  <svg v-if="isPublishing" class="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <span v-else>{{ t('upload.publish') }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { videoApi, type VideoCreateDTO } from '@/api'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'

const { t } = useI18n()
const emit = defineEmits(['publish-success', 'navigate'])

const isDragging = ref(false)
const isPublishing = ref(false)
const videoFile = ref<File | null>(null)
const coverFile = ref<File | null>(null)
const videoPreviewUrl = ref('')
const coverPreviewUrl = ref('')
const videoInput = ref<HTMLInputElement | null>(null)
const coverInput = ref<HTMLInputElement | null>(null)

const form = ref<VideoCreateDTO>({
  title: '',
  description: '',
  duration: 0,
  coverUrl: '',
  thumbnailUrl: ''
})

const isValid = computed(() => {
  return videoFile.value && form.value.title.trim().length > 0
})

const triggerVideoInput = () => videoInput.value?.click()
const triggerCoverInput = () => coverInput.value?.click()

const getVideoDuration = (file: File): Promise<number> => {
  return new Promise((resolve, reject) => {
    const video = document.createElement('video');
    video.preload = 'metadata';
    
    video.onloadedmetadata = function() {
      window.URL.revokeObjectURL(video.src);
      const duration = Math.floor(video.duration);
      resolve(duration);
    };
    
    video.onerror = function() {
      reject(new Error('无法读取视频信息'));
    };
    
    video.src = URL.createObjectURL(file);
  });
}

const handleVideoSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files?.length) {
    await processFile(target.files[0])
  }
}

const handleDrop = async (event: DragEvent) => {
  isDragging.value = false
  if (event.dataTransfer?.files.length) {
    await processFile(event.dataTransfer.files[0])
  }
}

const processFile = async (file: File) => {
  if (!file.type.startsWith('video/')) {
    ElMessage.error('请上传视频文件')
    return
  }
  videoFile.value = file
  videoPreviewUrl.value = URL.createObjectURL(file)
  
  // Auto fill title if empty
  if (!form.value.title) {
    form.value.title = file.name.replace(/\.[^/.]+$/, "")
  }

  // Get duration
  try {
    const duration = await getVideoDuration(file)
    form.value.duration = duration
  } catch (e) {
    console.error('Failed to get video duration', e)
    // Non-blocking error, but backend might reject if duration is 0
  }
}

const removeVideo = () => {
    videoFile.value = null
    videoPreviewUrl.value = ''
    form.value.duration = 0
    if(videoInput.value) videoInput.value.value = ''
}

const handleCoverSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files?.length) {
    const file = target.files[0]
    coverFile.value = file
    coverPreviewUrl.value = URL.createObjectURL(file)
  }
}

const handlePublish = async () => {
  if (!videoFile.value || !form.value.title) return

  if (form.value.duration <= 0) {
      // Try to get duration again if it failed or wasn't ready
      try {
          form.value.duration = await getVideoDuration(videoFile.value)
      } catch(e) {
          ElMessage.error('无法获取视频时长，请重试或更换视频')
          return
      }
  }

  isPublishing.value = true
  try {
    // 一次性上传视频和封面
    await videoApi.uploadVideo(videoFile.value, coverFile.value, form.value)
    
    ElMessage.success(t('upload.success'))
    emit('publish-success')
    
    // Reset form
    form.value = { title: '', description: '', duration: 0, coverUrl: '', thumbnailUrl: '' }
    removeVideo()
    coverFile.value = null
    coverPreviewUrl.value = ''

  } catch (error) {
    console.error('Publish failed:', error)
    ElMessage.error(t('upload.error'))
  } finally {
    isPublishing.value = false
  }
}
</script>


<style scoped>
.glass-panel {
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.05);
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.2);
}

:root[data-theme='light'] .glass-panel {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.05);
}

:root[data-theme='light'] .text-white {
  color: #1a1a1a;
}
:root[data-theme='light'] .border-white\/20 {
    border-color: rgba(0,0,0,0.1);
}
:root[data-theme='light'] .bg-white\/5 {
    background-color: rgba(0,0,0,0.05);
}
</style>
