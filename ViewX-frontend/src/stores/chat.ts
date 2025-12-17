import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { chatApi, type ConversationVO, type MessageVO } from '@/api'
import { webSocketService } from '@/utils/websocket'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores'

export const useChatStore = defineStore('chat', () => {
    const userStore = useUserStore()

    // 状态
    const conversations = ref<ConversationVO[]>([])
    const currentConversation = ref<ConversationVO | null>(null)
    const messages = ref<Map<number, MessageVO[]>>(new Map())
    const typingUsers = ref<Set<number>>(new Set())
    const connected = ref(false)
    const loading = ref(false)

    // 计算属性
    const totalUnreadCount = computed(() => {
        return conversations.value.reduce((sum, conv) => sum + conv.unreadCount, 0)
    })

    const currentMessages = computed(() => {
        if (!currentConversation.value) return []
        return messages.value.get(currentConversation.value.otherUserId) || []
    })

    const isTyping = computed(() => {
        if (!currentConversation.value) return false
        return typingUsers.value.has(currentConversation.value.otherUserId)
    })

    // 初始化 WebSocket
    async function initWebSocket() {
        if (connected.value) return

        const token = userStore.token
        if (!token) {
            console.warn('未登录，无法连接聊天服务')
            return
        }

        try {
            await webSocketService.connect(token)
            connected.value = true

            // 注册消息回调
            webSocketService.onMessage((message: MessageVO) => {
                handleNewMessage(message)
            })

            // 注册正在输入回调
            webSocketService.onTyping((userId: number) => {
                typingUsers.value.add(userId)
                setTimeout(() => {
                    typingUsers.value.delete(userId)
                }, 3000)
            })

            console.log('聊天服务已连接')
        } catch (error) {
            console.error('连接聊天服务失败:', error)
            ElMessage.error('聊天服务连接失败')
        }
    }

    // 处理新消息
    function handleNewMessage(message: MessageVO) {
        const otherUserId = message.senderId === userStore.userInfo?.id
            ? message.receiverId
            : message.senderId

        // 添加到消息列表
        const userMessages = messages.value.get(otherUserId) || []
        userMessages.push(message)
        messages.value.set(otherUserId, userMessages)

        // 更新会话列表
        updateConversationWithMessage(message)

        // 如果不是当前会话，显示通知
        if (currentConversation.value?.otherUserId !== otherUserId) {
            ElMessage.info(`${message.senderNickname}: ${message.content}`)
        }
    }

    // 更新会话列表
    function updateConversationWithMessage(message: MessageVO) {
        const otherUserId = message.senderId === userStore.userInfo?.id
            ? message.receiverId
            : message.senderId

        const index = conversations.value.findIndex(c => c.otherUserId === otherUserId)

        if (index !== -1) {
            const conv = conversations.value[index]
            conv.lastMessage = message.content
            conv.lastMessageTime = message.createdAt

            // 如果不是当前会话且消息是别人发的，增加未读数
            if (currentConversation.value?.otherUserId !== otherUserId &&
                message.receiverId === userStore.userInfo?.id) {
                conv.unreadCount++
            }

            // 移到列表顶部
            conversations.value.splice(index, 1)
            conversations.value.unshift(conv)
        } else {
            // 新会话，重新加载会话列表
            loadConversations()
        }
    }

    // 加载会话列表
    async function loadConversations() {
        try {
            loading.value = true
            const data = await chatApi.getConversations()
            conversations.value = data
        } catch (error) {
            console.error('加载会话列表失败:', error)
            ElMessage.error('加载会话列表失败')
        } finally {
            loading.value = false
        }
    }

    // 加载聊天历史
    async function loadChatHistory(otherUserId: number, page: number = 1) {
        try {
            loading.value = true
            const data = await chatApi.getChatHistory(otherUserId, page, 50)
            // 倒序排列（最新的在下面）
            const sortedMessages = data.reverse()
            messages.value.set(otherUserId, sortedMessages)
        } catch (error) {
            console.error('加载聊天历史失败:', error)
            ElMessage.error('加载聊天历史失败')
        } finally {
            loading.value = false
        }
    }

    // 发送消息
    async function sendMessage(receiverId: number, content: string) {
        if (!content.trim()) {
            ElMessage.warning('消息内容不能为空')
            return false
        }

        if (!connected.value) {
            ElMessage.warning('聊天服务未连接，请稍后重试')
            return false
        }

        try {
            // 通过 WebSocket 发送
            webSocketService.sendMessage(receiverId, content.trim())

            // 乐观更新：立即添加到本地消息列表
            const tempMessage: MessageVO = {
                id: Date.now(), // 临时 ID
                senderId: userStore.userInfo!.id,
                senderUsername: userStore.userInfo!.username,
                senderNickname: userStore.userInfo!.nickname || userStore.userInfo!.username,
                senderAvatar: userStore.userInfo!.avatar || '',
                receiverId,
                content: content.trim(),
                messageType: 'TEXT',
                isRead: false,
                createdAt: new Date().toISOString()
            }

            handleNewMessage(tempMessage)
            return true
        } catch (error) {
            console.error('发送消息失败:', error)
            ElMessage.error('发送消息失败')
            return false
        }
    }

    // 发送正在输入状态
    function sendTyping(receiverId: number) {
        if (connected.value) {
            webSocketService.sendTyping(receiverId)
        }
    }

    // 标记已读
    async function markAsRead(otherUserId: number) {
        try {
            await chatApi.markAsRead(otherUserId)

            // 更新本地会话未读数
            const conv = conversations.value.find(c => c.otherUserId === otherUserId)
            if (conv) {
                conv.unreadCount = 0
            }
        } catch (error) {
            console.error('标记已读失败:', error)
        }
    }

    // 选择会话
    async function selectConversation(conversation: ConversationVO) {
        currentConversation.value = conversation

        // 加载聊天历史
        if (!messages.value.has(conversation.otherUserId)) {
            await loadChatHistory(conversation.otherUserId)
        }

        // 标记已读
        if (conversation.unreadCount > 0) {
            await markAsRead(conversation.otherUserId)
        }
    }

    // 断开连接
    function disconnect() {
        webSocketService.disconnect()
        connected.value = false
    }

    // 清空当前会话
    function clearCurrentConversation() {
        currentConversation.value = null
    }

    return {
        // 状态
        conversations,
        currentConversation,
        messages,
        typingUsers,
        connected,
        loading,

        // 计算属性
        totalUnreadCount,
        currentMessages,
        isTyping,

        // 方法
        initWebSocket,
        loadConversations,
        loadChatHistory,
        sendMessage,
        sendTyping,
        markAsRead,
        selectConversation,
        disconnect,
        clearCurrentConversation
    }
})
