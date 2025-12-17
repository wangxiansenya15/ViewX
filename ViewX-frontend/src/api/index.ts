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
export interface VideoListVO {
    id: number
    title: string
    description?: string
    coverUrl?: string
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

export interface VideoVO {
    id: number
    title: string
    description?: string
    coverUrl?: string
    thumbnailUrl: string
    duration: number
    viewCount: number
    likeCount: number
    commentCount: number
    publishedAt: string
    uploaderId: number
    uploaderNickname: string
    uploaderAvatar: string
    videoUrl?: string // Added for feed playback
    tags?: string[] // Added for feed tags
}

// 视频详情 VO
export interface VideoDetailVO {
    id: number
    title: string
    description: string
    videoUrl: string
    coverUrl?: string
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
    duration: number
    description?: string
    coverUrl?: string
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
    videos?: VideoVO[]  // 用户的视频列表（可选）
}

// 关注状态 VO
export interface FollowStatusVO {
    isFollowing: boolean
    isFollower: boolean
    isMutual: boolean
    statusText: string  // "关注" | "已关注" | "相互关注"
}

// 内容上传 DTO
export interface ContentCreateDTO {
    title: string
    description?: string
    category?: string
    subcategory?: string
    tags?: string[]
    visibility?: 'PUBLIC' | 'PRIVATE' | 'UNLISTED'
}

// 内容详情 VO
export interface ContentDetailVO {
    id: number
    contentType: 'VIDEO' | 'IMAGE' | 'IMAGE_SET'
    title: string
    description?: string
    primaryUrl: string
    coverUrl?: string
    thumbnailUrl: string
    mediaUrls?: string[]  // 图片集使用
    duration?: number      // 视频使用
    category?: string
    subcategory?: string
    tags?: string[]
    visibility: string
    status: string
    uploaderId: number
    uploaderNickname: string
    uploaderAvatar: string
    isUploaderVerified: boolean
    viewCount: number
    likeCount: number
    dislikeCount: number
    shareCount: number
    commentCount: number
    isLiked: boolean
    isFavorited: boolean
    isFollowingUploader: boolean
    createdAt: string
    publishedAt: string
}

// 内容列表 VO
export interface ContentVO {
    id: number
    contentType: 'VIDEO' | 'IMAGE' | 'IMAGE_SET'
    title: string
    description?: string
    thumbnailUrl: string
    coverUrl?: string
    imageCount?: number    // 图片集的图片数量
    duration?: number      // 视频时长
    uploaderId: number
    uploaderNickname: string
    uploaderAvatar: string
    viewCount: number
    likeCount: number
    commentCount: number
    publishedAt: string
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

    // 获取我的视频
    getMyVideos() {
        return request.get<VideoVO[]>('/videos/my')
    },

    // 获取指定用户的视频
    getUserVideos(userId: number | string) {
        return request.get<VideoVO[]>(`/videos/user/${userId}`)
    },

    // 获取推荐视频流
    getFeed(page = 1, size = 10) {
        return request.get<VideoVO[]>('/recommend/feed', { params: { page, size } })
    },

    // 获取视频详情
    getDetail(id: number) {
        return request.get<VideoDetailVO>(`/videos/${id}`)
    },

    // 上传视频及元数据
    uploadVideo(file: File, coverFile: File | null, data: VideoCreateDTO) {
        const formData = new FormData()
        formData.append('file', file)

        // 如果有封面文件,一起上传
        if (coverFile) {
            formData.append('coverFile', coverFile)
        }

        formData.append('title', data.title)
        formData.append('duration', data.duration.toString())

        if (data.description) formData.append('description', data.description)
        if (data.coverUrl) formData.append('coverUrl', data.coverUrl)
        if (data.thumbnailUrl) formData.append('thumbnailUrl', data.thumbnailUrl)
        if (data.category) formData.append('category', data.category)
        if (data.subcategory) formData.append('subcategory', data.subcategory)
        if (data.visibility) formData.append('visibility', data.visibility)

        if (data.tags && data.tags.length > 0) {
            data.tags.forEach(tag => formData.append('tags', tag))
        }

        return request.post<number>('/videos', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    },

    // 上传封面图片(返回封面URL和缩略图URL)
    uploadCoverImage(file: File) {
        const formData = new FormData()
        formData.append('file', file)
        return request.post<{ coverUrl: string; thumbnailUrl: string }>('/videos/upload/cover', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })
    },

    // 更新视频
    updateVideo(id: number, data: Partial<VideoCreateDTO>) {
        return request.put<void>(`/videos/${id}`, data)
    },

    // 删除视频
    deleteVideo(id: number) {
        return request.delete<void>(`/videos/${id}`)
    }
}

// 用户摘要 VO (用于列表)
export interface UserSummaryVO {
    id: number
    username: string
    nickname: string
    avatar: string
    isFollowing: boolean
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
    },

    // 关注/取消关注用户
    toggleFollow(userId: number) {
        return request.post<string>(`/interactions/follow/${userId}`)
    },

    // 检查是否关注某用户
    isFollowing(userId: number) {
        return request.get<boolean>(`/interactions/follow/status/${userId}`)
    },

    // 获取详细的关注状态（包括相互关注）
    getDetailedFollowStatus(userId: number) {
        return request.get<FollowStatusVO>(`/interactions/follow/detailed-status/${userId}`)
    },

    // 获取用户的粉丝数和关注数
    getFollowStats(userId: number) {
        return request.get<{ followerCount: number; followingCount: number }>(`/interactions/follow/stats/${userId}`)
    },

    // 获取粉丝列表
    getFollowers(userId: number, page = 1, size = 20) {
        return request.get<UserSummaryVO[]>(`/interactions/followers/${userId}`, { params: { page, size } })
    },

    // 获取关注列表
    getFollowing(userId: number, page = 1, size = 20) {
        return request.get<UserSummaryVO[]>(`/interactions/following/${userId}`, { params: { page, size } })
    }
}

// 搜索 API
export const search = {
    // 搜索用户
    searchUsers(keyword: string, page = 1, size = 20) {
        return request.get<UserSummaryVO[]>('/users/search', {
            params: { keyword, page, size }
        })
    },

    // 搜索视频
    searchVideos(keyword: string, page = 1, size = 20) {
        return request.get<VideoListVO[]>('/recommend/search', {
            params: { keyword, page, size }
        })
    }
}

// 用户相关 API
export const userApi = {
    getMyProfile() {
        return request.get<UserProfileVO>('/user/profile/me')
    },
    getUserProfile(userId: number | string) {
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

// 内容相关 API (图片、图片集)
export const contentApi = {
    // 上传单张图片
    uploadImage(file: File, data: ContentCreateDTO) {
        const formData = new FormData()
        formData.append('file', file)
        formData.append('title', data.title)

        if (data.description) formData.append('description', data.description)
        if (data.category) formData.append('category', data.category)
        if (data.subcategory) formData.append('subcategory', data.subcategory)
        if (data.visibility) formData.append('visibility', data.visibility)

        if (data.tags && data.tags.length > 0) {
            data.tags.forEach(tag => formData.append('tags', tag))
        }

        return request.post<number>('/contents/image', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    },

    // 上传图片集 (2-9张图片)
    uploadImageSet(files: File[], data: ContentCreateDTO) {
        const formData = new FormData()

        // 添加所有图片文件
        files.forEach(file => {
            formData.append('files', file)
        })

        formData.append('title', data.title)

        if (data.description) formData.append('description', data.description)
        if (data.category) formData.append('category', data.category)
        if (data.subcategory) formData.append('subcategory', data.subcategory)
        if (data.visibility) formData.append('visibility', data.visibility)

        if (data.tags && data.tags.length > 0) {
            data.tags.forEach(tag => formData.append('tags', tag))
        }

        return request.post<number>('/contents/image-set', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    },

    // 获取内容详情
    getContentDetail(id: number, config?: any) {
        return request.get<ContentDetailVO>(`/contents/${id}`, config)
    },

    // 获取用户的内容列表
    getUserContents(userId: number, contentType?: 'VIDEO' | 'IMAGE' | 'IMAGE_SET') {
        return request.get<ContentVO[]>(`/contents/user/${userId}`, {
            params: contentType ? { type: contentType } : {}
        })
    },

    // 获取我的内容列表
    getMyContents(contentType?: 'VIDEO' | 'IMAGE' | 'IMAGE_SET') {
        return request.get<ContentVO[]>('/contents/my', {
            params: contentType ? { type: contentType } : {}
        })
    },

    // 删除内容
    deleteContent(id: number) {
        return request.delete<void>(`/contents/${id}`)
    }
}

// 导出系统 API
export { systemApi } from './system'
export type { SystemVersionInfo } from './system'

// ==================== 通知相关类型 ====================

export type NotificationType =
    | 'FOLLOW'
    | 'LIKE_VIDEO'
    | 'FAVORITE_VIDEO'
    | 'COMMENT_VIDEO'
    | 'REPLY_COMMENT'
    | 'LIKE_COMMENT'
    | 'VIDEO_APPROVED'
    | 'VIDEO_REJECTED'
    | 'SYSTEM_ANNOUNCEMENT'

export interface NotificationVO {
    id: number
    notificationType: NotificationType
    notificationTypeDesc: string
    senderId?: number
    senderUsername?: string
    senderNickname?: string
    senderAvatar?: string
    relatedVideoId?: number
    relatedVideoTitle?: string
    relatedVideoCover?: string
    relatedCommentId?: number
    relatedCommentContent?: string
    content?: string
    isRead: boolean
    createdAt: string
    timeDesc: string
}

export interface NotificationQueryDTO {
    notificationType?: NotificationType
    unreadOnly?: boolean
    page?: number
    pageSize?: number
}

// ==================== 通知 API ====================

export const notificationApi = {
    // 获取通知列表
    getNotifications(params?: NotificationQueryDTO) {
        return request.get<NotificationVO[]>('/notifications', { params })
    },

    // 获取未读通知数量
    getUnreadCount() {
        return request.get<number>('/notifications/unread-count')
    },

    // 标记通知为已读
    markAsRead(id: number) {
        return request.put<string>(`/notifications/${id}/read`)
    },

    // 标记所有通知为已读
    markAllAsRead() {
        return request.put<string>('/notifications/read-all')
    },

    // 删除通知
    deleteNotification(id: number) {
        return request.delete<string>(`/notifications/${id}`)
    }
}

// ==================== 聊天相关接口 ====================

// 会话 VO
export interface ConversationVO {
    conversationId: number
    otherUserId: number
    otherUserUsername: string
    otherUserNickname: string
    otherUserAvatar: string
    isOnline: boolean
    lastMessage: string
    lastMessageType: string
    lastMessageTime: string
    unreadCount: number
}

// 消息 VO
export interface MessageVO {
    id: number
    senderId: number
    senderUsername: string
    senderNickname: string
    senderAvatar: string
    receiverId: number
    content: string
    messageType: string
    isRead: boolean
    createdAt: string
}

// 聊天 API
export const chatApi = {
    // 获取会话列表
    getConversations() {
        return request.get<ConversationVO[]>('/messages/conversations')
    },

    // 获取聊天历史
    getChatHistory(otherUserId: number, page: number = 1, size: number = 50) {
        return request.get<MessageVO[]>(`/messages/history/${otherUserId}`, {
            params: { page, size }
        })
    },

    // 标记消息为已读
    markAsRead(otherUserId: number) {
        return request.put<void>(`/messages/read/${otherUserId}`)
    },

    // 获取未读消息总数
    getUnreadCount() {
        return request.get<number>('/messages/unread-count')
    }
}
