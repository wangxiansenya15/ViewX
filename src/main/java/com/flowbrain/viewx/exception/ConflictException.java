package com.flowbrain.viewx.exception;

/**
 * 用户操作冲突异常 (如重复点赞、重复注册)
 */
public class ConflictException extends BusinessException {
    public ConflictException(String message) {
        super(409, message);
    }
}
