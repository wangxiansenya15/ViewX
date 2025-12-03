import request from '@/utils/request'

// 用户登录 DTO
export interface LoginDTO {
    username?: string
    password?: string
    email?: string
}

// 注册 DTO (对应 User)
export interface RegisterDTO {
    username: string
    password?: string
    email: string
    verificationCode?: string // 前端需要传验证码
}

// 登录响应 VO
export interface LoginVO {
    token: string
    userInfo: {
        id: number
        username: string
        avatar: string
    }
}

// 视频 VO
export interface VideoVO {
    id: number
    title: string
    coverUrl: string
    videoUrl: string
    authorName: string
    viewCount: number
    createTime: string
}

export const authApi = {
    // 登录
    login(data: LoginDTO) {
        return request.post<LoginVO>('/auth/login', data)
    },

    // 注册
    register(data: RegisterDTO) {
        return request.post<void>('/auth/register', data)
    },

    // 获取验证码
    getVerificationCode(email: string) {
        return request.post<string>('/auth/code', { email })
    },

    // 校验验证码 (如果需要单独校验)
    verifyCode(email: string, code: string) {
        return request.post<void>('/auth/verify', { email, code })
    },

    // 验证 Token 是否有效
    validateToken(token: string) {
        return request.post<string>('/auth/validate', { token })
    },

    // 获取当前用户信息
    me() {
        return request.get<LoginVO['userInfo']>('/users/me')
    },

    // 退出登录
    logout() {
        return request.post<void>('/auth/logout')
    }
}

export const videoApi = {
    // 获取推荐视频列表
    getRecommended() {
        return request.get<VideoVO[]>('/videos/recommended')
    },

    // 获取视频详情
    getDetail(id: number) {
        return request.get<VideoVO>(`/videos/${id}`)
    }
}

// 用户资料 VO
export interface UserProfileVO {
    userId: number
    username: string
    nickname: string
    email: string
    phone: string
    avatarUrl: string
    description: string
    age: number
    gender: string
    address: string
    role: string
    createdAt: string
    followersCount: number
    followingCount: number
    videoCount: number
    likeCount: number
}

export const userApi = {
    // 获取当前用户资料
    getMyProfile() {
        return request.get<UserProfileVO>('/user/profile/me')
    },

    // 更新用户资料
    updateProfile(data: Partial<UserProfileVO>) {
        return request.put<UserProfileVO>('/user/profile/me', data)
    },

    // 上传头像
    uploadAvatar(file: File) {
        const formData = new FormData()
        formData.append('file', file)
        return request.post<string>('/user/profile/avatar', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })
    }
}
