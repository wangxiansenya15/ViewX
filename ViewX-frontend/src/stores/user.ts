import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface UserInfo {
    id: number
    username: string
    nickname: string
    avatar: string
    email?: string
}

export const useUserStore = defineStore('user', () => {
    const token = ref<string | null>(localStorage.getItem('token'))
    const userInfo = ref<UserInfo | null>(null)

    // 从 localStorage 加载用户信息
    const loadUserInfo = () => {
        const savedUserInfo = localStorage.getItem('userInfo')
        if (savedUserInfo) {
            try {
                userInfo.value = JSON.parse(savedUserInfo)
            } catch (error) {
                console.error('解析用户信息失败:', error)
            }
        }
    }

    // 初始化时加载
    loadUserInfo()

    // 计算属性
    const isLoggedIn = computed(() => !!token.value)

    // 设置 token
    function setToken(newToken: string) {
        token.value = newToken
        localStorage.setItem('token', newToken)
    }

    // 设置用户信息
    function setUserInfo(info: UserInfo) {
        userInfo.value = info
        localStorage.setItem('userInfo', JSON.stringify(info))
    }

    // 登出
    function logout() {
        token.value = null
        userInfo.value = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
    }

    return {
        token,
        userInfo,
        isLoggedIn,
        setToken,
        setUserInfo,
        logout,
        loadUserInfo
    }
})
