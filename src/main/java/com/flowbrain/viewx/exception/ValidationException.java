package com.flowbrain.viewx.exception;

/**
 * 参数验证异常
 * 当请求参数不符合业务规则时抛出
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}