import { defineStore } from 'pinia'
import { ref } from 'vue'
import { notificationApi, type NotificationVO, type NotificationQueryDTO } from '@/api'

export const useNotificationStore = defineStore('notification', () => {
    const notifications = ref<NotificationVO[]>([])
    const unreadCount = ref(0)
    const loading = ref(false)

    // 获取通知列表
    const fetchNotifications = async (params?: NotificationQueryDTO) => {
        loading.value = true
        try {
            const list = await notificationApi.getNotifications(params || { page: 1, pageSize: 10 })
            notifications.value = list
            return list
        } catch (error) {
            console.error('Failed to fetch notifications', error)
            throw error
        } finally {
            loading.value = false
        }
    }

    // 获取未读数量
    const fetchUnreadCount = async () => {
        try {
            unreadCount.value = await notificationApi.getUnreadCount()
        } catch (error) {
            console.error('Failed to fetch unread count', error)
        }
    }

    // 标记为已读
    const markAsRead = async (id: number) => {
        try {
            await notificationApi.markAsRead(id)
            const notification = notifications.value.find(n => n.id === id)
            if (notification && !notification.isRead) {
                notification.isRead = true
                unreadCount.value = Math.max(0, unreadCount.value - 1)
            }
        } catch (error) {
            console.error('Failed to mark as read', error)
            throw error
        }
    }

    // 标记所有为已读
    const markAllAsRead = async () => {
        try {
            await notificationApi.markAllAsRead()
            notifications.value.forEach(n => n.isRead = true)
            unreadCount.value = 0
        } catch (error) {
            console.error('Failed to mark all as read', error)
            throw error
        }
    }

    // 删除通知
    const deleteNotification = async (id: number) => {
        try {
            await notificationApi.deleteNotification(id)
            const index = notifications.value.findIndex(n => n.id === id)
            if (index !== -1) {
                const wasUnread = !notifications.value[index].isRead
                notifications.value.splice(index, 1)
                if (wasUnread) {
                    unreadCount.value = Math.max(0, unreadCount.value - 1)
                }
            }
        } catch (error) {
            console.error('Failed to delete notification', error)
            throw error
        }
    }

    // 添加新通知（用于 WebSocket 实时推送）
    const addNotification = (notification: NotificationVO) => {
        notifications.value.unshift(notification)
        if (!notification.isRead) {
            unreadCount.value++
        }
    }

    return {
        notifications,
        unreadCount,
        loading,
        fetchNotifications,
        fetchUnreadCount,
        markAsRead,
        markAllAsRead,
        deleteNotification,
        addNotification
    }
})
