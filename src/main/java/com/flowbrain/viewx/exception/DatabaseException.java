package com.flowbrain.viewx.exception;

/**
 * 数据库操作异常
 */
public class DatabaseException extends BusinessException {
    public DatabaseException(String message) {
        super(500, message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
