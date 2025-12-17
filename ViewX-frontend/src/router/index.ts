import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import MobileSearch from '../views/mobile/MobileSearch.vue'
import ProfileView from '../views/Profile.vue'
import UploadView from '../views/UploadVideo.vue'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'home',
            component: HomeView,
            meta: { keepAlive: true }
        },
        {
            path: '/search',
            name: 'search',
            component: () => import('../views/SearchResults.vue'),
            meta: { keepAlive: true }
        },
        {
            path: '/profile',
            name: 'profile',
            component: ProfileView,
            meta: { keepAlive: true }
        },
        {
            path: '/profile/:userId',
            name: 'user-profile',
            component: ProfileView,
            meta: { keepAlive: true }
        },
        {
            path: '/notifications',
            name: 'notifications',
            component: () => import('../views/Notifications.vue'),
            meta: { keepAlive: true }
        },
        {
            path: '/messages',
            name: 'messages',
            component: () => import('../views/Messages.vue'),
            meta: { keepAlive: true }
        },
        {
            path: '/upload',
            name: 'upload',
            component: UploadView
        },
        {
            path: '/settings',
            name: 'settings',
            component: () => import('../views/Settings.vue'),
            meta: { hideHeader: true }
        },
        {
            path: '/login',
            name: 'login',
            component: () => import('../views/Login.vue'),
            meta: { hideHeader: true, hideNav: true }
        },
        {
            path: '/oauth/callback',
            name: 'oauth-callback',
            component: () => import('../views/OAuthCallback.vue'),
            meta: { hideHeader: true, hideNav: true, hideSidebar: true }
        },
        {
            path: '/admin',
            name: 'admin',
            component: () => import('../views/admin/AdminLayout.vue'),
            meta: { hideHeader: true, hideNav: true, hideSidebar: true }
        }
    ]
})

export default router
