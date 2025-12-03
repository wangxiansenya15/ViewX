package com.flowbrain.viewx.exception;

/**
 * 外部服务调用异常 (如 AI API, 邮件服务)
 */
public class ExternalServiceException extends BusinessException {
    public ExternalServiceException(String message) {
        super(503, message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
