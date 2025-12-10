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

                // 假设 200 代表业务成功 (兼容字符串类型的状态码)
                if (Number(code) === 200 || Number(code) === 0) {
                    return data // 直接返回业务数据
                } else if (code === 401) {
                    ElMessage.error(message || 'Token已失效或过期，请重新登录')
                    localStorage.removeItem('token')
                    window.dispatchEvent(new Event('unauthorized'))
                    return Promise.reject(new Error(message || 'Token expired'))
                } else {
                    // 检查是否配置了跳过错误处理 (针对 HTTP 200 但业务 code 报错的情况)
                    const config = response.config as any;
                    const headers = config?.headers as any;
                    // 检查 headers (兼容 AxiosHeaders 实例)
                    const hasSkipHeader = headers?.['X-Skip-Error-Handler'] ||
                        headers?.['x-skip-error-handler'] ||
                        (typeof headers?.get === 'function' && (headers.get('X-Skip-Error-Handler') || headers.get('x-skip-error-handler')));

                    if (config?.skipErrorHandler || hasSkipHeader || config?.params?.['_skip_error']) {
                        return Promise.reject(new Error(message || 'Error'))
                    }

                    ElMessage.error(message || '系统错误')
                    return Promise.reject(new Error(message || 'Error'))
                }
            },
            (error: any) => {
                // 如果配置了跳过错误处理（支持通过 config, headers 或 params 传递）
                // 增加 headers 对象方法的检查(针对 AxiosHeaders 实例)
                const headers = error.config?.headers as any;
                const hasSkipHeader = headers?.['X-Skip-Error-Handler'] ||
                    headers?.['x-skip-error-handler'] ||
                    (typeof headers?.get === 'function' && (headers.get('X-Skip-Error-Handler') || headers.get('x-skip-error-handler')));

                if ((error.config as any)?.skipErrorHandler ||
                    hasSkipHeader ||
                    error.config?.params?.['_skip_error']) {
                    return Promise.reject(error)
                }

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
                        // 虽然 404 通常表示资源不存在，但有时也是业务逻辑的一部分（如检查用户是否存在）
                        // 如果没有 skipErrorHandler，才报错
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

    patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
        return this.instance.patch(url, data, config)
    }

    delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
        return this.instance.delete(url, config)
    }
}

import JSONBig from 'json-bigint'

// Configure JSONBig to store large numbers as strings
const JSONBigString = JSONBig({ storeAsString: true })

// 导出实例，baseURL 从环境变量获取
export default new Request({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 10000,
    transformResponse: [
        (data) => {
            try {
                // If data is a string, try to parse it with JSONBig
                if (typeof data === 'string') {
                    // console.log('Parsing JSON with JSONBig...')
                    return JSONBigString.parse(data)
                }
                return data
            } catch (e) {
                // If parsing fails, return original data
                return data
            }
        }
    ]
})
