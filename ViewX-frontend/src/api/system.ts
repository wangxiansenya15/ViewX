import request from '@/utils/request'

// 系统版本相关接口
export interface SystemVersionInfo {
    version: string
    buildTime: string
    updateLog: string
    forceUpdate: boolean
    downloadUrl?: string
    minCompatibleVersion: string
    updateType: 'MAJOR' | 'MINOR' | 'PATCH'
}

export const systemApi = {
    /**
     * 获取当前系统版本
     */
    async getVersion(): Promise<SystemVersionInfo> {
        const response = await request.get<SystemVersionInfo>('/system/version')
        return response
    },

    /**
     * 检查更新
     */
    async checkUpdate(clientVersion?: string): Promise<SystemVersionInfo | null> {
        const params = clientVersion ? { clientVersion } : {}
        const response = await request.get<SystemVersionInfo | null>('/system/check-update', { params })
        return response
    },

    /**
     * 执行版本升级
     */
    async performUpgrade(fromVersion: string, toVersion: string): Promise<void> {
        await request.post('/system/upgrade', null, {
            params: { fromVersion, toVersion }
        })
    },

    /**
     * 健康检查
     */
    async healthCheck(): Promise<string> {
        const response = await request.get<string>('/system/health')
        return response
    }
}
