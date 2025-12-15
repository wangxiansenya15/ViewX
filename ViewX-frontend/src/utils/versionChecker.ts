import { ElNotification, ElMessageBox } from 'element-plus'
import { systemApi, type SystemVersionInfo } from '@/api'

/**
 * 版本检查器类
 */
export class VersionChecker {
    private checkInterval: number = 5 * 60 * 1000 // 5分钟检查一次
    private timer: number | null = null
    private currentVersion: string
    private initialHash: string = ''

    constructor(version: string) {
        this.currentVersion = version
    }

    /**
     * 初始化版本检查
     */
    async init() {
        this.initialHash = await this.getCurrentBuildHash()
        this.startPeriodicCheck()
    }

    /**
     * 获取当前构建的哈希值（从HTML中提取）
     */
    private async getCurrentBuildHash(): Promise<string> {
        try {
            const response = await fetch('/index.html?' + Date.now(), {
                cache: 'no-cache'
            })
            const html = await response.text()
            // 从 HTML 中提取 JS 文件的哈希
            const match = html.match(/\/assets\/index-([a-f0-9]+)\.js/)
            return match ? match[1] : ''
        } catch (error) {
            console.error('获取构建哈希失败:', error)
            return ''
        }
    }

    /**
     * 开始定期检查
     */
    private startPeriodicCheck() {
        this.timer = window.setInterval(async () => {
            await this.checkForUpdates()
        }, this.checkInterval)
    }

    /**
     * 检查是否有更新
     */
    async checkForUpdates(): Promise<SystemVersionInfo | null> {
        try {
            // 方法1: 检查构建哈希变化
            const currentHash = await this.getCurrentBuildHash()
            if (currentHash && currentHash !== this.initialHash) {
                this.notifyHashChange()
                return null
            }

            // 方法2: 调用后端API检查版本
            const updateInfo = await systemApi.checkUpdate(this.currentVersion)

            if (updateInfo) {
                this.notifyVersionUpdate(updateInfo)
                return updateInfo
            }

            return null
        } catch (error) {
            console.error('检查更新失败:', error)
            return null
        }
    }

    /**
     * 手动检查更新（用于设置页面的"检查更新"按钮）
     */
    async manualCheck(): Promise<void> {
        try {
            const updateInfo = await systemApi.checkUpdate(this.currentVersion)

            if (updateInfo) {
                await this.showUpdateDialog(updateInfo)
            } else {
                ElNotification({
                    title: '已是最新版本',
                    message: `当前版本 ${this.currentVersion} 已是最新版本`,
                    type: 'success',
                    duration: 3000
                })
            }
        } catch (error) {
            console.error('检查更新失败:', error)
            ElNotification({
                title: '检查失败',
                message: '无法连接到服务器，请稍后重试',
                type: 'error',
                duration: 3000
            })
        }
    }

    /**
     * 通知构建哈希变化（静默更新）
     */
    private notifyHashChange() {
        ElNotification({
            title: '发现新版本',
            message: '检测到系统有更新，点击刷新页面以获得最佳体验',
            type: 'warning',
            duration: 0,
            showClose: true,
            onClick: () => {
                this.performUpdate()
            }
        })
    }

    /**
     * 通知版本更新
     */
    private notifyVersionUpdate(updateInfo: SystemVersionInfo) {
        const message = `
      <div style="line-height: 1.6;">
        <p><strong>新版本:</strong> ${updateInfo.version}</p>
        <p><strong>更新类型:</strong> ${this.getUpdateTypeLabel(updateInfo.updateType)}</p>
        ${updateInfo.updateLog ? `<p><strong>更新内容:</strong></p><pre style="white-space: pre-wrap; font-size: 12px;">${updateInfo.updateLog}</pre>` : ''}
      </div>
    `

        if (updateInfo.forceUpdate) {
            // 强制更新
            ElMessageBox.alert(message, '发现重要更新', {
                dangerouslyUseHTMLString: true,
                confirmButtonText: '立即更新',
                showClose: false,
                closeOnClickModal: false,
                closeOnPressEscape: false,
                callback: () => {
                    this.performUpdate(updateInfo)
                }
            })
        } else {
            // 可选更新
            ElNotification({
                title: '发现新版本',
                dangerouslyUseHTMLString: true,
                message: message,
                type: 'info',
                duration: 0,
                showClose: true,
                onClick: () => {
                    this.performUpdate(updateInfo)
                }
            })
        }
    }

    /**
     * 显示更新对话框（手动检查时使用）
     */
    private async showUpdateDialog(updateInfo: SystemVersionInfo): Promise<void> {
        const message = `
      <div style="line-height: 1.8;">
        <p><strong>当前版本:</strong> ${this.currentVersion}</p>
        <p><strong>最新版本:</strong> ${updateInfo.version}</p>
        <p><strong>更新类型:</strong> ${this.getUpdateTypeLabel(updateInfo.updateType)}</p>
        <p><strong>发布时间:</strong> ${updateInfo.buildTime}</p>
        ${updateInfo.updateLog ? `<div style="margin-top: 12px;"><strong>更新内容:</strong><pre style="background: #f5f5f5; padding: 12px; border-radius: 4px; margin-top: 8px; white-space: pre-wrap; font-size: 13px; color: #333;">${updateInfo.updateLog}</pre></div>` : ''}
      </div>
    `

        try {
            await ElMessageBox.confirm(message, '发现新版本', {
                dangerouslyUseHTMLString: true,
                confirmButtonText: '立即更新',
                cancelButtonText: updateInfo.forceUpdate ? '' : '稍后提醒',
                showCancelButton: !updateInfo.forceUpdate,
                closeOnClickModal: !updateInfo.forceUpdate,
                type: 'info'
            })

            this.performUpdate(updateInfo)
        } catch {
            // 用户取消更新
            console.log('用户取消更新')
        }
    }

    /**
     * 执行更新
     */
    private async performUpdate(updateInfo?: SystemVersionInfo) {
        if (updateInfo) {
            // 如果有版本信息，可以先调用升级接口
            try {
                await systemApi.performUpgrade(this.currentVersion, updateInfo.version)
            } catch (error) {
                console.error('执行升级逻辑失败:', error)
            }
        }

        // 刷新页面
        window.location.reload()
    }

    /**
     * 获取更新类型标签
     */
    private getUpdateTypeLabel(type: string): string {
        const labels: Record<string, string> = {
            'MAJOR': '主版本更新',
            'MINOR': '功能更新',
            'PATCH': '补丁更新'
        }
        return labels[type] || '更新'
    }

    /**
     * 停止检查
     */
    stop() {
        if (this.timer) {
            clearInterval(this.timer)
            this.timer = null
        }
    }

    /**
     * 比较版本号
     */
    static compareVersion(v1: string, v2: string): number {
        const parts1 = v1.split('.').map(Number)
        const parts2 = v2.split('.').map(Number)

        const maxLength = Math.max(parts1.length, parts2.length)

        for (let i = 0; i < maxLength; i++) {
            const num1 = parts1[i] || 0
            const num2 = parts2[i] || 0

            if (num1 > num2) return 1
            if (num1 < num2) return -1
        }

        return 0
    }
}

// 创建全局版本检查器实例
let versionCheckerInstance: VersionChecker | null = null

/**
 * 获取版本检查器实例
 */
export function getVersionChecker(version: string = '1.0.0'): VersionChecker {
    if (!versionCheckerInstance) {
        versionCheckerInstance = new VersionChecker(version)
    }
    return versionCheckerInstance
}

/**
 * 销毁版本检查器实例
 */
export function destroyVersionChecker() {
    if (versionCheckerInstance) {
        versionCheckerInstance.stop()
        versionCheckerInstance = null
    }
}
