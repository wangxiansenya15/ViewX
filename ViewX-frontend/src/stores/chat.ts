import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import { chatApi, type ConversationVO, type MessageVO } from '@/api'
import { webSocketService } from '@/utils/websocket'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores'

export const useChatStore = defineStore('chat', () => {
    const userStore = useUserStore()

    // 状态
    const conversations = ref<ConversationVO[]>([])
    const currentConversation = ref<ConversationVO | null>(null)
    const messages = reactive(new Map<string, MessageVO[]>())
    // 强制更新触发器
    const msgUpdateTrigger = ref(0)

    // 正在输入的用户的 ID 集合
    const typingUsers = ref<Set<string>>(new Set())
    const connected = ref(false)
    const loading = ref(false)

    // 计算属性
    const totalUnreadCount = computed(() => {
        return conversations.value.reduce((sum, conv) => sum + conv.unreadCount, 0)
    })

    const currentMessages = computed(() => {
        // 依赖这个触发器
        msgUpdateTrigger.value

        if (!currentConversation.value) {
            return []
        }
        // 统一转换为字符串作为键
        const userId = currentConversation.value.otherUserId.toString()
        const msgs = messages.get(userId) || []

        console.log('🔍 [currentMessages] 触发器值:', msgUpdateTrigger.value)
        console.log('🔍 [currentMessages] 当前会话 otherUserId:', userId, '类型:', typeof userId)
        console.log('🔍 [currentMessages] Map 中的所有键:', Array.from(messages.keys()))
        console.log('🔍 [currentMessages] 获取到的消息数:', msgs.length)
        if (msgs.length > 0) {
            console.log('🔍 [currentMessages] 最后一条消息:', msgs[msgs.length - 1])
        }

        return msgs
    })

    const isTyping = computed(() => {
        if (!currentConversation.value) return false
        // 统一转换为字符串作为键
        const userId = currentConversation.value.otherUserId.toString()
        return typingUsers.value.has(userId)
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
            webSocketService.onTyping(handleTyping)

            // 注册撤回回调
            webSocketService.onRecall((notification) => {
                handleRecallNotification(notification)
            })

            // 注册删除回调
            webSocketService.onDelete((notification) => {
                handleDeleteNotification(notification)
            })

            console.log('聊天服务已连接')
        } catch (error) {
            console.error('连接聊天服务失败:', error)
            // 不在这里显示错误提示，由 websocket.ts 统一处理
        }
    }

    // 处理撤回通知
    function handleRecallNotification(notification: { messageId: number | string }) {
        const msgIdStr = notification.messageId.toString()
        console.log('🔄 处理撤回通知:', msgIdStr)

        // 遍历所有会话的消息列表查找并更新
        messages.forEach((msgs, userId) => {
            const msg = msgs.find(m => m.id.toString() === msgIdStr)
            if (msg) {
                msg.isRecalled = true
                msg.recalledAt = new Date().toISOString()
                // 强制更新
                msgUpdateTrigger.value++

                // 更新会话列表显示（无论是不是最后一条消息）
                const conv = conversations.value.find(c => c.otherUserId.toString() === userId)
                if (conv) {
                    // 判断是自己还是对方撤回的
                    const isMine = msg.senderId.toString() === userStore.userInfo?.id?.toString()
                    conv.lastMessage = isMine ? '你撤回了一条消息' : `${conv.otherUserNickname}撤回了一条消息`
                    // 更新时间为撤回时间
                    conv.lastMessageTime = new Date().toISOString()
                }
            }
        })
    }

    // 处理删除通知
    function handleDeleteNotification(notification: { messageId: number | string }) {
        const msgIdStr = notification.messageId.toString()
        console.log('🗑️ 处理删除通知:', msgIdStr)

        // 遍历所有会话的消息列表查找并删除
        messages.forEach((msgs, userId) => {
            const index = msgs.findIndex(m => m.id.toString() === msgIdStr)
            if (index !== -1) {
                msgs.splice(index, 1)
                // 强制更新
                msgUpdateTrigger.value++
            }
        })
    }

    // 处理新消息
    function handleNewMessage(message: MessageVO) {
        const otherUserId = message.senderId.toString() === userStore.userInfo?.id?.toString()
            ? message.receiverId
            : message.senderId
        const otherUserIdStr = otherUserId.toString()
        const isMine = message.senderId.toString() === userStore.userInfo?.id?.toString()

        console.log('📩 处理新消息:', message)

        // 消息列表
        const oldMessages = messages.get(otherUserIdStr) || []

        // 强化的去重逻辑
        if (isMine) {
            // 倒序查找（从最新的找起）
            // 匹配条件：内容相同 + (是临时消息 OR 也就是最后一条消息)
            // 临时消息特征：ID 是时间戳 (13位)，而真实消息 ID 是雪花算法 (19位)
            // 我们放宽条件：只要 ID 长度小于 16 或者是字符串形式的数字，就认为是临时的
            let tempMessageIndex = -1

            for (let i = oldMessages.length - 1; i >= 0; i--) {
                const m = oldMessages[i]

                // 1. 内容必须相同
                if (m.content !== message.content) continue

                // 2. 必须是同一个人的（其实外层 isMine 已经保证了，但保险起见）
                if (m.senderId.toString() !== message.senderId.toString()) continue

                // 3. 判断是否为临时消息：ID 长度不同，或者 ID 相等（极端情况）
                // Date.now() 长度 13
                // Snowflake 长度 ~19
                const mIdStr = m.id.toString()
                const newIdStr = message.id.toString()

                // 如果 ID 完全相等，说明已经处理过了（或者后端回传了临时ID），直接返回
                if (mIdStr === newIdStr) {
                    console.log('✅ 消息 ID 完全相同，跳过:', mIdStr)
                    return
                }

                // 如果旧消息 ID 长度 < 16 (认为是时间戳)，则认为是临时消息
                if (mIdStr.length < 16) {
                    tempMessageIndex = i
                    break // 找到最近的一条就停止
                }
            }

            if (tempMessageIndex !== -1) {
                console.log('🔄 替换临时消息:', oldMessages[tempMessageIndex].id, '->', message.id)
                const newMessages = [...oldMessages]
                newMessages[tempMessageIndex] = message
                messages.set(otherUserIdStr, newMessages)
            } else {
                console.log('➕ 未找到临时消息，添加新消息 (ID长度:', message.id.toString().length, ')')
                const newMessages = [...oldMessages, message]
                messages.set(otherUserIdStr, newMessages)
            }
        } else {
            // 别人发的消息，检查 ID 是否重复
            const isDuplicate = oldMessages.some(m => m.id.toString() === message.id.toString())
            if (!isDuplicate) {
                const newMessages = [...oldMessages, message]
                messages.set(otherUserIdStr, newMessages)
            }
        }

        // 触发强制更新
        msgUpdateTrigger.value++

        // 更新会话列表
        updateConversationWithMessage(message)

        // 如果不是当前会话，显示通知
        const isCurrentConversation = currentConversation.value &&
            currentConversation.value.otherUserId.toString() === otherUserId.toString()

        if (!isCurrentConversation && !isMine) {
            // 页面内通知
            ElMessage.info(`${message.senderNickname}: ${message.content}`)

            // 浏览器桌面通知
            showDesktopNotification(message)
        }
    }

    // 显示桌面通知
    function showDesktopNotification(message: MessageVO) {
        // 检查浏览器是否支持通知
        if (!('Notification' in window)) {
            return
        }

        // 请求通知权限（如果还没有）
        if (Notification.permission === 'default') {
            Notification.requestPermission()
            return
        }

        // 如果已授权，显示通知
        if (Notification.permission === 'granted') {
            const notification = new Notification(message.senderNickname || '新消息', {
                body: message.content.substring(0, 50) + (message.content.length > 50 ? '...' : ''),
                icon: message.senderAvatar || '/favicon.ico',
                tag: `chat-${message.senderId}`, // 同一发送者的通知会替换旧的
                requireInteraction: false,
                silent: false
            })

            // 点击通知时聚焦窗口
            notification.onclick = () => {
                window.focus()
                notification.close()
            }

            // 3秒后自动关闭
            setTimeout(() => notification.close(), 3000)
        }
    }

    // 处理正在输入状态
    function handleTyping(userId: number) {
        const userIdStr = userId.toString()
        typingUsers.value.add(userIdStr)
        setTimeout(() => {
            typingUsers.value.delete(userIdStr)
        }, 3000)
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

            // 调试：打印会话数据
            console.log('📋 加载会话列表:', data.length, '个会话')
            data.forEach((conv, index) => {
                console.log(`会话 ${index + 1}:`, {
                    nickname: conv.otherUserNickname,
                    avatar: conv.otherUserAvatar,
                    avatarExists: !!conv.otherUserAvatar
                })
            })
        } catch (error) {
            console.error('加载会话列表失败:', error)
            ElMessage.error('加载会话列表失败')
        } finally {
            loading.value = false
        }
    }

    // 加载聊天历史
    async function loadChatHistory(otherUserId: number | string, page: number = 1) {
        try {
            loading.value = true
            // API 调用支持 string，直接传递
            const data = await chatApi.getChatHistory(otherUserId, page, 50)
            // 倒序排列（最新的在下面）
            const sortedMessages = data.reverse()
            // 使用字符串作为键存储
            messages.set(otherUserId.toString(), sortedMessages)
        } catch (error) {
            console.error('加载聊天历史失败:', error)
            ElMessage.error('加载聊天历史失败')
        } finally {
            loading.value = false
        }
    }

    // 发送消息
    async function sendMessage(receiverId: number | string, content: string) {
        if (!content.trim()) {
            ElMessage.warning('消息内容不能为空')
            return false
        }

        if (!connected.value) {
            ElMessage.warning('聊天服务未连接，请稍后重试')
            return false
        }

        // 检查用户信息是否存在
        if (!userStore.userInfo) {
            ElMessage.error('用户信息未加载，请重新登录')
            return false
        }

        try {
            // 通过 WebSocket 发送（直接使用原始 ID，避免大整数精度丢失）
            console.log('chatStore: 发送消息给:', receiverId)
            webSocketService.sendMessage(receiverId, content.trim())

            // 乐观更新：立即添加到本地消息列表
            const tempMessage: MessageVO = {
                id: Date.now(), // 临时 ID
                senderId: userStore.userInfo.id,
                senderUsername: userStore.userInfo.username,
                senderNickname: userStore.userInfo.nickname || userStore.userInfo.username,
                senderAvatar: userStore.userInfo.avatar || '',
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
    function sendTyping(receiverId: number | string) {
        if (connected.value) {
            // 直接传递，不转换，避免大整数精度丢失
            webSocketService.sendTyping(receiverId)
        }
    }

    // 标记已读
    async function markAsRead(otherUserId: number | string) {
        try {
            await chatApi.markAsRead(otherUserId)

            // 更新本地会话未读数
            const conv = conversations.value.find(c => c.otherUserId.toString() === otherUserId.toString())
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
        const userIdStr = conversation.otherUserId.toString()
        if (!messages.has(userIdStr)) {
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

    // 更新会话的用户信息（头像、昵称）
    function updateConversationProfile(userId: string | number, nickname: string, avatar: string) {
        const userIdStr = userId.toString()
        const conv = conversations.value.find(c => c.otherUserId.toString() === userIdStr)
        if (conv) {
            if (nickname) conv.otherUserNickname = nickname
            if (avatar) conv.otherUserAvatar = avatar
        }

        // 如果当前会话也是这个用户，同步更新
        if (currentConversation.value && currentConversation.value.otherUserId.toString() === userIdStr) {
            if (nickname) currentConversation.value.otherUserNickname = nickname
            if (avatar) currentConversation.value.otherUserAvatar = avatar
        }
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
        clearCurrentConversation,
        updateConversationProfile
    }
})
