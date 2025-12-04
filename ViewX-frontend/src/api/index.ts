import request from '@/utils/request'

// 用户登录 DTO
export interface LoginDTO {
    username?: string
    password?: string
    email?: string
}

// 注册 DTO
export interface RegisterDTO {
    username: string
    password?: string
    email: string
    verificationCode?: string
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

// 视频列表 VO
export interface VideoVO {
    id: number
    title: string
    thumbnailUrl: string
    duration: number
    viewCount: number
    likeCount: number
    commentCount: number
    publishedAt: string
    uploaderId: number
    uploaderNickname: string
    uploaderAvatar: string
}

// 视频详情 VO
export interface VideoDetailVO {
    id: number
    title: string
    description: string
    videoUrl: string
    thumbnailUrl: string
    duration: number
    category: string
    subcategory: string
    tags: string[]
    viewCount: number
    likeCount: number
    dislikeCount: number
    shareCount: number
    commentCount: number
    uploaderId: number
    uploaderNickname: string
    uploaderAvatar: string
    isUploaderVerified: boolean
    isLiked: boolean
    isFavorited: boolean
    isFollowingUploader: boolean
    publishedAt: string
    createdAt: string
}

// 视频创建 DTO
export interface VideoCreateDTO {
    title: string
    description?: string
    videoUrl: string
    thumbnailUrl?: string
    category?: string
    subcategory?: string
    tags?: string[]
    visibility?: 'PUBLIC' | 'PRIVATE' | 'UNLISTED'
}

// 视频更新 DTO
export interface VideoUpdateDTO {
    title?: string
    description?: string
    thumbnailUrl?: string
    category?: string
    subcategory?: string
    tags?: string[]
    visibility?: 'PUBLIC' | 'PRIVATE' | 'UNLISTED'
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

// 认证相关 API
export const authApi = {
    login(data: LoginDTO) {
        return request.post<LoginVO>('/auth/login', data)
    },
    register(data: RegisterDTO) {
        return request.post<void>('/auth/register', data)
    },
    getVerificationCode(email: string) {
        return request.post<string>('/auth/code', { email })
    },
    verifyCode(email: string, code: string) {
        return request.post<void>('/auth/verify', { email, code })
    },
    validateToken(token: string) {
        return request.post<string>('/auth/validate', { token })
    },
    me() {
        return request.get<LoginVO['userInfo']>('/users/me')
    },
    logout() {
        return request.post<void>('/auth/logout')
    }
}

// 视频相关 API
export const videoApi = {
    // 获取热门视频
    getTrending(page = 1, size = 10) {
        return request.get<VideoVO[]>('/recommend/trending', { params: { page, size } })
    },

    // 获取推荐视频流
    getFeed(page = 1, size = 10) {
        return request.get<VideoVO[]>('/recommend/feed', { params: { page, size } })
    },

    // 获取视频详情
    getDetail(id: number) {
        return request.get<VideoDetailVO>(`/videos/${id}`)
    },

    // 上传视频文件
    uploadVideoFile(file: File) {
        const formData = new FormData()
        formData.append('file', file)
        return request.post<string>('/videos/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    },

    // 上传封面图片
    uploadCoverImage(file: File) {
        const formData = new FormData()
        formData.append('file', file)
        return request.post<string>('/videos/upload/cover', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    },

    // 创建视频
    createVideo(data: VideoCreateDTO) {
        return request.post<number>('/videos', data)
    },

    // 更新视频
    updateVideo(id: number, data: VideoUpdateDTO) {
        return request.put<string>(`/videos/${id}`, data)
    }
}

// 交互相关 API
export const interactionApi = {
    // 点赞/取消点赞
    toggleLike(videoId: number) {
        return request.post<void>(`/interactions/like/${videoId}`)
    },

    // 收藏/取消收藏
    toggleFavorite(videoId: number) {
        return request.post<void>(`/interactions/favorite/${videoId}`)
    },

    // 获取交互状态
    getStatus(videoId: number) {
        return request.get<{ liked: boolean; favorited: boolean }>(`/interactions/status/${videoId}`)
    }
}

// 用户相关 API
export const userApi = {
    getMyProfile() {
        return request.get<UserProfileVO>('/user/profile/me')
    },
    getUserProfile(userId: number) {
        return request.get<UserProfileVO>(`/user/profile/${userId}`)
    },
    updateProfile(data: Partial<UserProfileVO>) {
        return request.put<UserProfileVO>('/user/profile/me', data)
    },
    uploadAvatar(file: File) {
        const formData = new FormData()
        formData.append('file', file)
        return request.post<string>('/user/profile/avatar', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    }
}

// 管理员相关 API
export const adminApi = {
    // 获取用户列表
    getUsers(page = 1, size = 10) {
        return request.get<any>('/admin/users', { params: { page, size } })
    },

    // 管理员为用户上传视频
    createVideoForUser(userId: number, data: VideoCreateDTO) {
        return request.post<number>(`/admin/users/${userId}/videos`, data)
    },

    // 管理员更新用户信息
    updateUser(userId: number, data: any) {
        return request.put<void>(`/admin/users/${userId}`, { id: userId, ...data })
    },

    // 管理员更新用户状态
    updateUserStatus(userId: number, status: string) {
        return request.patch<void>(`/admin/users/${userId}/status`, { status })
    }
}
