import SockJS from 'sockjs-client'
import { Client, IMessage } from '@stomp/stompjs'
import { ElMessage } from 'element-plus'

export interface ChatMessage {
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

export interface Notification {
    id: number
    recipientId: number
    senderId: number
    senderNickname: string
    senderAvatar: string
    notificationType: string
    notificationTypeDesc: string
    relatedVideoId?: number
    relatedCommentId?: number
    content: string
    isRead: boolean
    createdAt: string
    timeDesc: string
}

export interface MessageActionNotification {
    messageId: number | string
    userId: number | string
    type: string // "MESSAGE_RECALLED" | "MESSAGE_DELETED"
}

class WebSocketService {
    private client: Client | null = null
    private connected = false
    private reconnectAttempts = 0
    private maxReconnectAttempts = 5
    private reconnectDelay = 3000
    private hasShownReconnectError = false  // 是否已显示重连失败提示
    private messageCallbacks: ((message: ChatMessage) => void)[] = []
    private typingCallbacks: ((userId: number) => void)[] = []
    private recallCallbacks: ((notification: MessageActionNotification) => void)[] = []
    private deleteCallbacks: ((notification: MessageActionNotification) => void)[] = []
    private notificationCallbacks: ((notification: Notification) => void)[] = []
    private connectCallbacks: (() => void)[] = []
    private errorCallbacks: ((error: any) => void)[] = []

    /**
     * 连接 WebSocket
     */
    connect(token: string): Promise<void> {
        return new Promise((resolve, reject) => {
            if (this.connected) {
                resolve()
                return
            }

            const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
            // SockJS requires HTTP/HTTPS URL, not WS/WSS
            const wsUrl = apiBaseUrl + '/ws'

            this.client = new Client({
                webSocketFactory: () => new SockJS(wsUrl),
                connectHeaders: {
                    Authorization: `Bearer ${token}`
                },
                debug: (str) => {
                    // console.log('[WebSocket Debug]', str)
                },
                reconnectDelay: 0,  // 禁用 STOMP 自动重连，使用我们自己的重连逻辑
                heartbeatIncoming: 4000,
                heartbeatOutgoing: 4000,
                onConnect: () => {
                    console.log('WebSocket 连接成功')
                    this.connected = true
                    this.reconnectAttempts = 0
                    this.hasShownReconnectError = false  // 重置错误提示标志
                    this.subscribe()
                    this.connectCallbacks.forEach(cb => cb())
                    resolve()
                },
                onStompError: (frame) => {
                    console.error('WebSocket STOMP 错误:', frame)
                    this.connected = false
                    // 只在第一次错误时显示提示
                    if (!this.hasShownReconnectError) {
                        reject(new Error(frame.headers['message'] || 'WebSocket 连接失败'))
                    }
                },
                onWebSocketClose: () => {
                    console.log('WebSocket 连接关闭')
                    this.connected = false
                    this.attemptReconnect(token)
                }
            })

            this.client.activate()
        })
    }

    /**
     * 订阅消息频道
     */
    private subscribe() {
        if (!this.client) return

        console.log('=== 开始订阅 WebSocket 频道 ===')

        // 订阅个人消息队列
        this.client.subscribe('/user/queue/messages', (message: IMessage) => {
            try {
                // ... 保持原有逻辑
                let chatMessage: ChatMessage
                const parsed = JSON.parse(message.body)
                if (typeof parsed === 'string') {
                    chatMessage = JSON.parse(parsed)
                } else {
                    chatMessage = parsed
                }
                this.messageCallbacks.forEach(cb => cb(chatMessage))
            } catch (error) {
                console.error('❌ 解析消息失败:', error)
            }
        })
        console.log('✅ 已订阅: /user/queue/messages')

        // 订阅撤回通知
        this.client.subscribe('/user/queue/recall', (message: IMessage) => {
            try {
                const notification: MessageActionNotification = JSON.parse(message.body)
                console.log('🔄 收到撤回通知:', notification)
                this.recallCallbacks.forEach(cb => cb(notification))
            } catch (error) {
                console.error('❌ 解析撤回通知失败:', error)
            }
        })
        console.log('✅ 已订阅: /user/queue/recall')

        // 订阅删除通知
        this.client.subscribe('/user/queue/delete', (message: IMessage) => {
            try {
                const notification: MessageActionNotification = JSON.parse(message.body)
                console.log('🗑️ 收到删除通知:', notification)
                this.deleteCallbacks.forEach(cb => cb(notification))
            } catch (error) {
                console.error('❌ 解析删除通知失败:', error)
            }
        })
        console.log('✅ 已订阅: /user/queue/delete')

        // 订阅正在输入通知
        this.client.subscribe('/user/queue/typing', (message: IMessage) => {
            try {
                const userId = parseInt(message.body)
                this.typingCallbacks.forEach(cb => cb(userId))
            } catch (error) {
                console.error('❌ 解析正在输入通知失败:', error)
            }
        })
        console.log('✅ 已订阅: /user/queue/typing')


        // 订阅错误消息
        this.client.subscribe('/user/queue/errors', (message: IMessage) => {
            try {
                const errorData = JSON.parse(message.body)
                console.error('❌ 收到错误消息:', errorData)

                // 显示错误提示
                ElMessage.error(errorData.message || '操作失败')

                // 调用错误回调
                this.errorCallbacks.forEach(cb => cb(errorData))
            } catch (error) {
                console.error('❌ 解析错误消息失败:', error)
            }
        })
        console.log('✅ 已订阅: /user/queue/errors')

        // 订阅通知消息
        const userId = localStorage.getItem('userId')
        if (userId) {
            this.client.subscribe(`/topic/notifications/${userId}`, (message: IMessage) => {
                try {
                    const notification: Notification = JSON.parse(message.body)
                    console.log('🔔 收到新通知:', notification)
                    this.notificationCallbacks.forEach(cb => cb(notification))
                } catch (error) {
                    console.error('❌ 解析通知失败:', error)
                }
            })
            console.log(`✅ 已订阅: /topic/notifications/${userId}`)
        }

        // 发送连接确认
        console.log('📤 发送连接确认...')
        this.send('/app/chat.connect', {})
        console.log('=== WebSocket 频道订阅完成 ===')
    }

    /**
     * 尝试重新连接
     */
    private attemptReconnect(token: string) {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('WebSocket 重连次数已达上限')
            // 只显示一次错误提示
            if (!this.hasShownReconnectError) {
                ElMessage.error('聊天服务连接失败，请刷新页面重试')
                this.hasShownReconnectError = true
            }
            return
        }

        this.reconnectAttempts++
        console.log(`尝试重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`)

        setTimeout(() => {
            this.connect(token).catch(error => {
                console.error('重连失败:', error)
            })
        }, this.reconnectDelay * this.reconnectAttempts)
    }

    /**
     * 发送消息
     */
    /**
     * 发送消息
     */
    sendMessage(receiverId: number | string, content: string, messageType: string = 'TEXT') {
        if (!this.connected || !this.client) {
            ElMessage.warning('聊天服务未连接，请稍后重试')
            return
        }

        console.log('📤 WebSocket 发送消息:', { receiverId, content, type: typeof receiverId })

        this.send('/app/chat.send', {
            receiverId, // 直接发送，如果是字符串，JSON 会序列化为字符串
            content,
            messageType
        })
    }

    /**
     * 发送正在输入状态
     */
    /**
     * 发送正在输入状态
     */
    sendTyping(receiverId: number | string) {
        if (!this.connected || !this.client) return
        console.log('📤 发送正在输入状态:', receiverId)
        this.send('/app/chat.typing', receiverId)
    }

    /**
     * 通用发送方法
     */
    private send(destination: string, body: any) {
        if (!this.client) return

        try {
            this.client.publish({
                destination,
                body: JSON.stringify(body)
            })
        } catch (error) {
            console.error('发送消息失败:', error)
            ElMessage.error('发送失败，请重试')
        }
    }

    /**
     * 注册消息回调
     */
    onMessage(callback: (message: ChatMessage) => void) {
        this.messageCallbacks.push(callback)
    }

    /**
     * 注册正在输入回调
     */
    onTyping(callback: (userId: number) => void) {
        this.typingCallbacks.push(callback)
    }

    /**
     * 注册撤回回调
     */
    onRecall(callback: (notification: MessageActionNotification) => void) {
        this.recallCallbacks.push(callback)
    }

    /**
     * 注册删除回调
     */
    onDelete(callback: (notification: MessageActionNotification) => void) {
        this.deleteCallbacks.push(callback)
    }

    /**
     * 注册通知回调
     */
    onNotification(callback: (notification: Notification) => void) {
        this.notificationCallbacks.push(callback)
    }

    /**
     * 注册连接成功回调
     */
    onConnect(callback: () => void) {
        this.connectCallbacks.push(callback)
    }

    /**
     * 注册错误回调
     */
    onError(callback: (error: any) => void) {
        this.errorCallbacks.push(callback)
    }

    /**
     * 断开连接
     */
    disconnect() {
        if (this.client) {
            this.client.deactivate()
            this.client = null
            this.connected = false
            this.messageCallbacks = []
            this.typingCallbacks = []
            this.recallCallbacks = []
            this.deleteCallbacks = []
            this.notificationCallbacks = []
            this.connectCallbacks = []
            this.errorCallbacks = []
            console.log('WebSocket 已断开')
        }
    }

    /**
     * 检查连接状态
     */
    isConnected(): boolean {
        return this.connected
    }
}

// 导出单例
export const webSocketService = new WebSocketService()
