<template>
  <div class="min-h-screen bg-[#0f0f0f] pt-24 pb-10 px-4 sm:px-6 lg:px-8">
    <div class="max-w-4xl mx-auto">
      
      <div class="glass-panel rounded-3xl p-8 relative overflow-hidden">
        <!-- Background Decoration -->
        <div class="absolute top-0 right-0 -mr-20 -mt-20 w-96 h-96 bg-pink-600/10 rounded-full blur-3xl"></div>
        <div class="absolute bottom-0 left-0 -ml-20 -mb-20 w-80 h-80 bg-purple-600/10 rounded-full blur-3xl"></div>
        
        <div class="relative z-10">
          <h1 class="text-2xl font-bold text-white mb-8 flex items-center">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 mr-3 text-pink-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            {{ uploadType === 'image' ? '上传图片' : '上传图片集' }}
          </h1>

          <!-- 上传类型切换 -->
          <div class="flex gap-4 mb-6">
            <button 
              @click="uploadType = 'image'" 
              class="px-6 py-2 rounded-full transition-all"
              :class="uploadType === 'image' ? 'bg-pink-600 text-white' : 'bg-white/10 text-gray-400 hover:bg-white/20'"
            >
              单张图片
            </button>
            <button 
              @click="uploadType = 'image-set'" 
              class="px-6 py-2 rounded-full transition-all"
              :class="uploadType === 'image-set' ? 'bg-pink-600 text-white' : 'bg-white/10 text-gray-400 hover:bg-white/20'"
            >
              图片集 (2-9张)
            </button>
          </div>

          <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
            <!-- Left: Image Upload -->
            <div class="space-y-6">
              <!-- 单张图片上传 -->
              <div v-if="uploadType === 'image'">
                <div 
                  class="border-2 border-dashed border-white/20 rounded-2xl h-64 flex flex-col items-center justify-center p-6 transition-colors cursor-pointer"
                  :class="{'border-pink-500 bg-pink-500/10': isDragging, 'hover:border-white/40 hover:bg-white/5': !imageFile}"
                  @dragover.prevent="isDragging = true"
                  @dragleave.prevent="isDragging = false"
                  @drop.prevent="handleDrop"
                  @click="triggerImageInput"
                >
                  <input type="file" ref="imageInput" class="hidden" accept="image/*" @change="handleImageSelect" />
                  
                  <template v-if="!imageFile">
                    <div class="w-16 h-16 rounded-full bg-white/10 flex items-center justify-center mb-4">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-pink-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                      </svg>
                    </div>
                    <p class="text-white font-medium mb-1">拖拽图片到这里</p>
                    <p class="text-gray-500 text-sm">支持 JPG, PNG, GIF, WEBP</p>
                  </template>

                  <template v-else>
                    <img :src="imagePreviewUrl" class="w-full h-full object-contain rounded-lg" />
                  </template>
                </div>
                <button v-if="imageFile" @click.stop="removeImage" class="text-red-400 hover:text-red-300 text-sm mt-2 underline">移除图片</button>
              </div>

              <!-- 图片集上传 -->
              <div v-else>
                <div 
                  class="border-2 border-dashed border-white/20 rounded-2xl p-6 transition-colors cursor-pointer min-h-[16rem]"
                  :class="{'border-pink-500 bg-pink-500/10': isDragging, 'hover:border-white/40 hover:bg-white/5': imageFiles.length === 0}"
                  @dragover.prevent="isDragging = true"
                  @dragleave.prevent="isDragging = false"
                  @drop.prevent="handleDropMultiple"
                  @click="triggerImageSetInput"
                >
                  <input type="file" ref="imageSetInput" class="hidden" accept="image/*" multiple @change="handleImageSetSelect" />
                  
                  <template v-if="imageFiles.length === 0">
                    <div class="flex flex-col items-center justify-center py-8">
                      <div class="w-16 h-16 rounded-full bg-white/10 flex items-center justify-center mb-4">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-pink-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                        </svg>
                      </div>
                      <p class="text-white font-medium mb-1">选择 2-9 张图片</p>
                      <p class="text-gray-500 text-sm">支持 JPG, PNG, GIF, WEBP</p>
                    </div>
                  </template>

                  <template v-else>
                    <div class="grid grid-cols-3 gap-2">
                      <div v-for="(preview, index) in imagePreviewUrls" :key="index" class="relative group">
                        <img :src="preview" class="w-full h-24 object-cover rounded-lg" />
                        <button 
                          @click.stop="removeImageFromSet(index)" 
                          class="absolute top-1 right-1 w-6 h-6 bg-red-500 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                        >
                          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                          </svg>
                        </button>
                      </div>
                    </div>
                    <p class="text-gray-400 text-sm mt-4">已选择 {{ imageFiles.length }} 张图片</p>
                  </template>
                </div>
              </div>
            </div>

            <!-- Right: Metadata Form -->
            <div class="space-y-6">
              <div>
                <label class="block text-sm font-medium text-gray-400 mb-2">标题</label>
                <input 
                  v-model="form.title" 
                  type="text" 
                  class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-600 focus:outline-none focus:border-pink-500 focus:bg-white/10 transition-all"
                  placeholder="给图片起个标题..."
                />
              </div>

              <div>
                <label class="block text-sm font-medium text-gray-400 mb-2">描述</label>
                <textarea 
                  v-model="form.description" 
                  rows="6" 
                  class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-600 focus:outline-none focus:border-pink-500 focus:bg-white/10 transition-all resize-none"
                  placeholder="描述一下这张图片..."
                ></textarea>
              </div>

              <div class="pt-4">
                <button 
                  @click="handlePublish" 
                  :disabled="isPublishing || !isValid"
                  class="w-full py-3.5 bg-gradient-to-r from-pink-600 to-purple-600 rounded-xl text-white font-bold shadow-lg shadow-pink-500/20 hover:shadow-pink-500/40 hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:translate-y-0 transition-all flex items-center justify-center gap-2"
                >
                  <svg v-if="isPublishing" class="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <span v-else>发布</span>
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
import { contentApi, type ContentCreateDTO } from '@/api'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['publish-success'])

const uploadType = ref<'image' | 'image-set'>('image')
const isDragging = ref(false)
const isPublishing = ref(false)

// 单张图片
const imageFile = ref<File | null>(null)
const imagePreviewUrl = ref('')
const imageInput = ref<HTMLInputElement | null>(null)

// 图片集
const imageFiles = ref<File[]>([])
const imagePreviewUrls = ref<string[]>([])
const imageSetInput = ref<HTMLInputElement | null>(null)

const form = ref<ContentCreateDTO>({
  title: '',
  description: ''
})

const isValid = computed(() => {
  if (uploadType.value === 'image') {
    return imageFile.value && form.value.title.trim().length > 0
  } else {
    return imageFiles.value.length >= 2 && imageFiles.value.length <= 9 && form.value.title.trim().length > 0
  }
})

const triggerImageInput = () => imageInput.value?.click()
const triggerImageSetInput = () => imageSetInput.value?.click()

const handleImageSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files?.length) {
    processImage(target.files[0])
  }
}

const handleDrop = (event: DragEvent) => {
  isDragging.value = false
  if (event.dataTransfer?.files.length) {
    processImage(event.dataTransfer.files[0])
  }
}

const processImage = (file: File) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件')
    return
  }
  
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 10MB')
    return
  }
  
  imageFile.value = file
  imagePreviewUrl.value = URL.createObjectURL(file)
  
  if (!form.value.title) {
    form.value.title = file.name.replace(/\.[^/.]+$/, "")
  }
}

const removeImage = () => {
  imageFile.value = null
  imagePreviewUrl.value = ''
  if (imageInput.value) imageInput.value.value = ''
}

const handleImageSetSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files) {
    processImageSet(Array.from(target.files))
  }
}

const handleDropMultiple = (event: DragEvent) => {
  isDragging.value = false
  if (event.dataTransfer?.files) {
    processImageSet(Array.from(event.dataTransfer.files))
  }
}

const processImageSet = (files: File[]) => {
  const validFiles = files.filter(file => {
    if (!file.type.startsWith('image/')) {
      ElMessage.error(`${file.name} 不是图片文件`)
      return false
    }
    if (file.size > 10 * 1024 * 1024) {
      ElMessage.error(`${file.name} 大小超过 10MB`)
      return false
    }
    return true
  })
  
  if (validFiles.length < 2) {
    ElMessage.error('至少需要选择 2 张图片')
    return
  }
  
  if (validFiles.length > 9) {
    ElMessage.error('最多只能选择 9 张图片')
    validFiles.splice(9)
  }
  
  imageFiles.value = validFiles
  imagePreviewUrls.value = validFiles.map(file => URL.createObjectURL(file))
  
  if (!form.value.title) {
    form.value.title = `图片集 - ${new Date().toLocaleDateString()}`
  }
}

const removeImageFromSet = (index: number) => {
  imageFiles.value.splice(index, 1)
  URL.revokeObjectURL(imagePreviewUrls.value[index])
  imagePreviewUrls.value.splice(index, 1)
}

const handlePublish = async () => {
  if (!isValid.value) return

  isPublishing.value = true
  try {
    if (uploadType.value === 'image' && imageFile.value) {
      await contentApi.uploadImage(imageFile.value, form.value)
      ElMessage.success('图片发布成功!')
    } else if (uploadType.value === 'image-set' && imageFiles.value.length > 0) {
      await contentApi.uploadImageSet(imageFiles.value, form.value)
      ElMessage.success('图片集发布成功!')
    }
    
    emit('publish-success')
    
    // Reset form
    form.value = { title: '', description: '' }
    removeImage()
    imageFiles.value = []
    imagePreviewUrls.value = []

  } catch (error) {
    console.error('Publish failed:', error)
    ElMessage.error('发布失败，请重试')
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
</style>
