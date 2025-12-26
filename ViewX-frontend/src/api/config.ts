import request from '@/utils/request';

/**
 * 配置管理 API
 * 仅管理员可访问
 */

// 验证配置管理密码
export const verifyConfigPassword = (password: string) => {
    return request.post('/admin/config/verify-password', { password });
};

// 获取所有可编辑的配置项 (仅Key)
export const getEditableConfigKeys = () => {
    return request.get<string[]>('/admin/config/keys');
};

// 更新单个配置
export const updateConfig = (key: string, value: string) => {
    return request.put(`/admin/config/${key}`, { value });
};

// 批量更新配置
export const updateConfigs = (configs: Record<string, string>) => {
    return request.put('/admin/config/batch', configs);
};

// 快速更新邮箱授权码
export const updateMailPassword = (password: string) => {
    return request.put('/admin/config/mail-password', { password });
};

// 备份配置
export const backupConfig = () => {
    return request.post('/admin/config/backup');
};

// 重启应用
export const restartApplication = () => {
    return request.post('/admin/config/restart');
};
