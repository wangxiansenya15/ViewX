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

class WebSocketService {
    private client: Client | null = null
    private connected = false
    private reconnectAttempts = 0
    private maxReconnectAttempts = 5
    private reconnectDelay = 3000
    private messageCallbacks: ((message: ChatMessage) => void)[] = []
    private typingCallbacks: ((userId: number) => void)[] = []
    private connectCallbacks: (() => void)[] = []

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
            const wsUrl = apiBaseUrl.replace(/^http/, 'ws') + '/ws'

            this.client = new Client({
                webSocketFactory: () => new SockJS(wsUrl),
                connectHeaders: {
                    Authorization: `Bearer ${token}`
                },
                debug: (str) => {
                    console.log('[WebSocket Debug]', str)
                },
                reconnectDelay: this.reconnectDelay,
                heartbeatIncoming: 4000,
                heartbeatOutgoing: 4000,
                onConnect: () => {
                    console.log('WebSocket 连接成功')
                    this.connected = true
                    this.reconnectAttempts = 0
                    this.subscribe()
                    this.connectCallbacks.forEach(cb => cb())
                    resolve()
                },
                onStompError: (frame) => {
                    console.error('WebSocket STOMP 错误:', frame)
                    this.connected = false
                    reject(new Error(frame.headers['message'] || 'WebSocket 连接失败'))
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

        // 订阅个人消息队列
        this.client.subscribe('/user/queue/messages', (message: IMessage) => {
            try {
                const chatMessage: ChatMessage = JSON.parse(message.body)
                console.log('收到新消息:', chatMessage)
                this.messageCallbacks.forEach(cb => cb(chatMessage))
            } catch (error) {
                console.error('解析消息失败:', error)
            }
        })

        // 订阅正在输入通知
        this.client.subscribe('/user/queue/typing', (message: IMessage) => {
            try {
                const userId = parseInt(message.body)
                this.typingCallbacks.forEach(cb => cb(userId))
            } catch (error) {
                console.error('解析正在输入通知失败:', error)
            }
        })

        // 发送连接确认
        this.send('/app/chat.connect', {})
    }

    /**
     * 尝试重新连接
     */
    private attemptReconnect(token: string) {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('WebSocket 重连次数已达上限')
            ElMessage.error('聊天服务连接失败，请刷新页面重试')
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
    sendMessage(receiverId: number, content: string, messageType: string = 'TEXT') {
        if (!this.connected || !this.client) {
            ElMessage.warning('聊天服务未连接，请稍后重试')
            return
        }

        this.send('/app/chat.send', {
            receiverId,
            content,
            messageType
        })
    }

    /**
     * 发送正在输入状态
     */
    sendTyping(receiverId: number) {
        if (!this.connected || !this.client) return
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
     * 注册连接成功回调
     */
    onConnect(callback: () => void) {
        this.connectCallbacks.push(callback)
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
            this.connectCallbacks = []
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
