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
            component: HomeView
        },
        {
            path: '/search',
            name: 'search',
            component: MobileSearch
        },
        {
            path: '/profile',
            name: 'profile',
            component: ProfileView
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
        // Add other routes as needed
    ]
})

export default router
