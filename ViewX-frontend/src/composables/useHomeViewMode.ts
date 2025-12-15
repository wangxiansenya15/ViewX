import { ref, watch } from 'vue'

const VIEW_MODE_KEY = 'viewx_home_view_mode'

export type ViewMode = 'grid' | 'feed'

const viewMode = ref<ViewMode>((localStorage.getItem(VIEW_MODE_KEY) as ViewMode) || 'grid')

watch(viewMode, (newMode) => {
    localStorage.setItem(VIEW_MODE_KEY, newMode)
})

export function useHomeViewMode() {
    const toggleViewMode = () => {
        viewMode.value = viewMode.value === 'grid' ? 'feed' : 'grid'
    }

    const setViewMode = (mode: ViewMode) => {
        viewMode.value = mode
    }

    return {
        viewMode,
        toggleViewMode,
        setViewMode
    }
}
