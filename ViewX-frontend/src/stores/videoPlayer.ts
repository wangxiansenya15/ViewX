import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useVideoPlayerStore = defineStore('videoPlayer', () => {
    // 全局音量 (0-1)
    const volume = ref(1)

    // 全局播放速度
    const playbackRate = ref(1)

    // 是否静音
    const isMuted = ref(false)

    // 设置音量
    const setVolume = (newVolume: number) => {
        volume.value = Math.max(0, Math.min(1, newVolume))
        // 保存到 localStorage
        localStorage.setItem('viewx_video_volume', volume.value.toString())
    }

    // 设置播放速度
    const setPlaybackRate = (rate: number) => {
        playbackRate.value = rate
        localStorage.setItem('viewx_video_playback_rate', rate.toString())
    }

    // 切换静音
    const toggleMute = () => {
        isMuted.value = !isMuted.value
        localStorage.setItem('viewx_video_muted', isMuted.value.toString())
    }

    // 从 localStorage 恢复设置
    const restoreSettings = () => {
        const savedVolume = localStorage.getItem('viewx_video_volume')
        const savedRate = localStorage.getItem('viewx_video_playback_rate')
        const savedMuted = localStorage.getItem('viewx_video_muted')

        if (savedVolume) volume.value = parseFloat(savedVolume)
        if (savedRate) playbackRate.value = parseFloat(savedRate)
        if (savedMuted) isMuted.value = savedMuted === 'true'
    }

    return {
        volume,
        playbackRate,
        isMuted,
        setVolume,
        setPlaybackRate,
        toggleMute,
        restoreSettings
    }
})
