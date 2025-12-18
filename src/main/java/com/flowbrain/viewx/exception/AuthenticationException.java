package com.flowbrain.viewx.exception;

/**
 * 认证异常
 * 当用户认证失败时抛出
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}