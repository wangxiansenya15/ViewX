/**
 * 视频上传示例代码
 * 展示如何在前端实现完整的视频上传流程
 */

// ============= 工具函数 =============

/**
 * 获取视频时长（秒）
 * @param {File} file - 视频文件
 * @returns {Promise<number>} 视频时长（秒）
 */
export function getVideoDuration(file: File): Promise<number> {
    return new Promise((resolve, reject) => {
        const video = document.createElement('video');
        video.preload = 'metadata';

        video.onloadedmetadata = function () {
            window.URL.revokeObjectURL(video.src);
            const duration = Math.floor(video.duration);
            resolve(duration);
        };

        video.onerror = function () {
            reject(new Error('无法读取视频信息'));
        };

        video.src = URL.createObjectURL(file);
    });
}

/**
 * 从视频生成封面图片
 * @param {File} file - 视频文件
 * @param {number} timeInSeconds - 截取时间点（秒）
 * @returns {Promise<File>} 封面图片文件
 */
export function generateCoverFromVideo(file: File, timeInSeconds: number = 1): Promise<File> {
    return new Promise((resolve, reject) => {
        const video = document.createElement('video');
        const canvas = document.createElement('canvas');
        const context = canvas.getContext('2d');

        if (!context) {
            reject(new Error('无法创建canvas上下文'));
            return;
        }

        video.onloadedmetadata = function () {
            // 确保时间点不超过视频长度
            video.currentTime = Math.min(timeInSeconds, video.duration - 0.1);
        };

        video.onseeked = function () {
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            context.drawImage(video, 0, 0, canvas.width, canvas.height);

            canvas.toBlob(function (blob) {
                if (!blob) {
                    reject(new Error('无法生成封面图片'));
                    return;
                }

                window.URL.revokeObjectURL(video.src);
                const coverFile = new File([blob], 'cover.jpg', { type: 'image/jpeg' });
                resolve(coverFile);
            }, 'image/jpeg', 0.9);
        };

        video.onerror = function () {
            reject(new Error('无法加载视频'));
        };

        video.src = URL.createObjectURL(file);
    });
}

/**
 * 格式化视频时长为 HH:MM:SS
 * @param {number} seconds - 秒数
 * @returns {string} 格式化后的时长
 */
export function formatDuration(seconds: number): string {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    if (hours > 0) {
        return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

// ============= API 调用函数 =============

/**
 * 上传封面图片
 * @param {File} coverFile - 封面图片文件
 * @param {string} token - 认证令牌
 * @returns {Promise<string>} 封面图片URL
 */
export async function uploadCoverImage(coverFile: File, token: string): Promise<string> {
    const formData = new FormData();
    formData.append('file', coverFile);

    const response = await fetch('/videos/upload/cover', {
        method: 'POST',
        body: formData,
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });

    if (!response.ok) {
        throw new Error('封面上传失败');
    }

    const result = await response.json();

    if (result.code !== 200) {
        throw new Error(result.message || '封面上传失败');
    }

    return result.data;
}

/**
 * 上传视频及元数据
 * @param {Object} params - 上传参数
 * @returns {Promise<number>} 视频ID
 */
export async function uploadVideo(params: {
    videoFile: File;
    title: string;
    duration: number;
    description?: string;
    category?: string;
    subcategory?: string;
    coverUrl?: string;
    tags?: string[];
    visibility?: 'PUBLIC' | 'PRIVATE' | 'UNLISTED';
    token: string;
}): Promise<number> {
    const formData = new FormData();

    // 必填字段
    formData.append('file', params.videoFile);
    formData.append('title', params.title);
    formData.append('duration', params.duration.toString());

    // 可选字段
    if (params.description) {
        formData.append('description', params.description);
    }
    if (params.category) {
        formData.append('category', params.category);
    }
    if (params.subcategory) {
        formData.append('subcategory', params.subcategory);
    }
    if (params.coverUrl) {
        formData.append('coverUrl', params.coverUrl);
    }
    if (params.tags && params.tags.length > 0) {
        params.tags.forEach(tag => {
            formData.append('tags', tag);
        });
    }
    if (params.visibility) {
        formData.append('visibility', params.visibility);
    }

    const response = await fetch('/videos', {
        method: 'POST',
        body: formData,
        headers: {
            'Authorization': `Bearer ${params.token}`
        }
    });

    if (!response.ok) {
        throw new Error('视频上传失败');
    }

    const result = await response.json();

    if (result.code !== 200) {
        throw new Error(result.message || '视频上传失败');
    }

    return result.data;
}

// ============= 完整上传流程示例 =============

/**
 * 完整的视频上传流程
 * @param {File} videoFile - 视频文件
 * @param {Object} metadata - 视频元数据
 * @param {string} token - 认证令牌
 * @param {Function} onProgress - 进度回调
 * @returns {Promise<number>} 视频ID
 */
export async function uploadVideoComplete(
    videoFile: File,
    metadata: {
        title: string;
        description?: string;
        category?: string;
        tags?: string[];
        visibility?: 'PUBLIC' | 'PRIVATE' | 'UNLISTED';
    },
    token: string,
    onProgress?: (step: string, progress: number) => void
): Promise<number> {
    try {
        // 步骤1: 解析视频时长
        onProgress?.('解析视频信息', 10);
        const duration = await getVideoDuration(videoFile);
        console.log('视频时长:', formatDuration(duration));

        // 步骤2: 生成封面图片
        onProgress?.('生成封面图片', 30);
        const coverFile = await generateCoverFromVideo(videoFile, 1);
        console.log('封面生成成功');

        // 步骤3: 上传封面
        onProgress?.('上传封面', 50);
        const coverUrl = await uploadCoverImage(coverFile, token);
        console.log('封面URL:', coverUrl);

        // 步骤4: 上传视频
        onProgress?.('上传视频', 70);
        const videoId = await uploadVideo({
            videoFile,
            title: metadata.title,
            duration,
            description: metadata.description,
            category: metadata.category,
            coverUrl,
            tags: metadata.tags,
            visibility: metadata.visibility || 'PUBLIC',
            token
        });

        onProgress?.('上传完成', 100);
        console.log('视频ID:', videoId);

        return videoId;

    } catch (error) {
        console.error('视频上传失败:', error);
        throw error;
    }
}

// ============= Vue 组件使用示例 =============

/*
<template>
  <div class="video-upload">
    <input 
      type="file" 
      accept="video/*" 
      @change="handleFileSelect"
      ref="fileInput"
    />
    
    <div v-if="uploading" class="upload-progress">
      <p>{{ uploadStep }}</p>
      <div class="progress-bar">
        <div 
          class="progress-fill" 
          :style="{ width: uploadProgress + '%' }"
        ></div>
      </div>
      <p>{{ uploadProgress }}%</p>
    </div>
    
    <div v-if="videoId" class="upload-success">
      <p>上传成功！视频ID: {{ videoId }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { uploadVideoComplete } from './videoUploadExample';

const fileInput = ref(null);
const uploading = ref(false);
const uploadStep = ref('');
const uploadProgress = ref(0);
const videoId = ref(null);

async function handleFileSelect(event) {
  const file = event.target.files[0];
  if (!file) return;
  
  uploading.value = true;
  videoId.value = null;
  
  try {
    const token = localStorage.getItem('token'); // 获取认证令牌
    
    const id = await uploadVideoComplete(
      file,
      {
        title: '我的视频 #测试',
        description: '这是一个测试视频 #编程',
        category: 'Education',
        tags: ['教程', '编程'],
        visibility: 'PUBLIC'
      },
      token,
      (step, progress) => {
        uploadStep.value = step;
        uploadProgress.value = progress;
      }
    );
    
    videoId.value = id;
    
  } catch (error) {
    alert('上传失败: ' + error.message);
  } finally {
    uploading.value = false;
    if (fileInput.value) {
      fileInput.value.value = '';
    }
  }
}
</script>

<style scoped>
.video-upload {
  padding: 20px;
}

.upload-progress {
  margin-top: 20px;
}

.progress-bar {
  width: 100%;
  height: 20px;
  background-color: #f0f0f0;
  border-radius: 10px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background-color: #4CAF50;
  transition: width 0.3s ease;
}

.upload-success {
  margin-top: 20px;
  padding: 10px;
  background-color: #d4edda;
  border: 1px solid #c3e6cb;
  border-radius: 5px;
  color: #155724;
}
</style>
*/
