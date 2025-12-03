import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

// 定义后端统一返回结果格式 (根据 Java 后端 Result 包装类调整)
// 假设 Java 后端返回格式为: { code: 200, message: "success", data: T }
export interface Result<T = any> {
    code: number
    message: string
    data: T
}

class Request {
    private instance: AxiosInstance

    constructor(config: AxiosRequestConfig) {
        this.instance = axios.create(config)

        // 请求拦截器
        this.instance.interceptors.request.use(
            (config: InternalAxiosRequestConfig) => {
                // 从 localStorage 获取 token (假设 key 为 'token')
                const token = localStorage.getItem('token')
                if (token) {
                    // Java Spring Security 通常使用 Bearer Token
                    config.headers.Authorization = `Bearer ${token}`
                }
                return config
            },
            (error: any) => {
                return Promise.reject(error)
            }
        )

        // 响应拦截器
        this.instance.interceptors.response.use(
            (response: AxiosResponse) => {
                const { code, message, data } = response.data as Result

                // 假设 200 代表业务成功
                if (code === 200) {
                    return data // 直接返回业务数据
                } else if (code === 401) {
                    ElMessage.error(message || 'Token已失效或过期，请重新登录')
                    localStorage.removeItem('token')
                    window.dispatchEvent(new Event('unauthorized'))
                    return Promise.reject(new Error(message || 'Token expired'))
                } else {
                    ElMessage.error(message || '系统错误')
                    return Promise.reject(new Error(message || 'Error'))
                }
            },
            (error: any) => {
                // 处理 HTTP 状态码错误
                let message = ''
                const status = error.response?.status
                switch (status) {
                    case 401:
                        message = '未授权，请重新登录'
                        localStorage.removeItem('token')
                        window.dispatchEvent(new Event('unauthorized'))
                        break
                    case 403:
                        message = '拒绝访问'
                        break
                    case 404:
                        message = '请求地址出错'
                        break
                    case 500:
                        message = '服务器内部错误'
                        break
                    default:
                        message = '网络连接故障'
                }
                ElMessage.error(message)
                return Promise.reject(error)
            }
        )
    }

    // 封装常用方法
    get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
        return this.instance.get(url, config)
    }

    post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
        return this.instance.post(url, data, config)
    }

    put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
        return this.instance.put(url, data, config)
    }

    delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
        return this.instance.delete(url, config)
    }
}

// 导出实例，baseURL 从环境变量获取
export default new Request({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 10000,
})
