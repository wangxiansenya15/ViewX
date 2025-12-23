import axios from 'axios'

// Create a specific instance for Actuator which returns standard JSON, not wrapped in {code, data, message}
const actuatorRequest = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 10000
})

// Add token interceptor
actuatorRequest.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => Promise.reject(error)
)

// Simple response interceptor
actuatorRequest.interceptors.response.use(
    (response) => {
        return response.data
    },
    (error) => {
        return Promise.reject(error)
    }
)

export const getSystemHealth = () => {
    return actuatorRequest.get('/actuator/health')
}

export const getSystemInfo = () => {
    return actuatorRequest.get('/actuator/info')
}

// Metrics
export const getSystemMetrics = (metricName: string) => {
    return actuatorRequest.get(`/actuator/metrics/${metricName}`)
}

export const getEnv = () => {
    return actuatorRequest.get('/actuator/env')
}

// Loggers
export const getLoggers = () => {
    return actuatorRequest.get('/actuator/loggers')
}

export const updateLoggerLevel = (loggerName: string, level: string) => {
    return actuatorRequest.post(`/actuator/loggers/${loggerName}`, {
        configuredLevel: level
    })
}

// Threads
export const getThreadDump = () => {
    return actuatorRequest.get('/actuator/threaddump')
}

// Mappings
export const getMappings = () => {
    return actuatorRequest.get('/actuator/mappings')
}

// Beans
export const getBeans = () => {
    return actuatorRequest.get('/actuator/beans')
}
