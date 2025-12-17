package com.flowbrain.viewx.common;

import cn.hutool.http.HttpStatus;
import lombok.Data;

/**
 * 通用结果对象
 * 
 * @param <T> 数据泛型
 *
 * @author: arthur wang
 * @date: 2025/5/10
 *
 */

@Data
public final class Result<T> {
    private int code;
    private String message;
    private T data;

    // HTTP 状态码常量
    /**
     * 请求成功处理的状态码 200
     */
    public static final int OK = HttpStatus.HTTP_OK;

    /**
     * 请求参数错误，或者资源不存在的状态码 400
     */
    public static final int BAD_REQUEST = HttpStatus.HTTP_BAD_REQUEST;

    /**
     * 认证失败的状态码 401
     */
    public static final int UNAUTHORIZED = HttpStatus.HTTP_UNAUTHORIZED;

    /**
     * 拒绝访问的状态码 403
     */
    public static final int FORBIDDEN = HttpStatus.HTTP_FORBIDDEN;

    /**
     * 资源不存在的状态码 404
     */
    public static final int NOT_FOUND = HttpStatus.HTTP_NOT_FOUND;

    /**
     * 服务器内部错误，或者资源不存在的状态码 500
     */
    public static final int SERVER_ERROR = HttpStatus.HTTP_INTERNAL_ERROR;

    // 私有构造方法
    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ------------ 成功方法 ------------

    /**
     * 成功方法，自定义状态码和消息
     * 
     * @param <T>     泛型类型
     *
     * @param message 自定义消息
     * @return 成功的结果对象
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(OK, message, null);
    }

    /**
     * 成功方法，返回200状态码和消息，自定义数据
     * 
     * @param <T>  泛型类型
     * @param data 自定义数据
     * @return 默认成功结果对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(OK, "操作成功", data);
    }

    /**
     * 成功方法，返回200状态码，自定义消息和数据
     * 
     * @param <T>     泛型类型
     * @param message 自定义消息
     * @param data    自定义数据
     * @return 默认成功结果对象
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(OK, message, data);
    }

    /**
     * 默认成功方法，返回200状态码，默认消息，不带数据
     */
    public static Result<Void> success() {
        return success("操作成功", null);
    }

    // ------------ 失败方法 ------------

    /**
     * 失败方法，自定义状态码和消息
     * 
     * @param <T>     泛型类型
     * @param code    自定义状态码
     * @param message 自定义消息
     * @return 失败的结果对象
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    // ------------ 快捷方法 ------------

    /**
     * 快捷失败方法 返回400状态码 错误请求
     * 
     * @param message 错误信息
     */
    public static <T> Result<T> badRequest(String message) {
        return error(BAD_REQUEST, message);
    }

    /**
     * 快捷失败方法 返回401状态码 未授权
     * 
     * @param message 错误信息
     */
    public static <T> Result<T> unauthorized(String message) {
        return error(UNAUTHORIZED, message);
    }

    /**
     * 快捷失败方法 返回403状态码 拒绝访问
     * 
     * @param message 错误信息
     */
    public static <T> Result<T> forbidden(String message) {
        return error(FORBIDDEN, message);
    }

    /**
     * 快捷失败方法 返回404状态码 未找到资源
     * 
     * @param message 错误信息
     */
    public static <T> Result<T> notFound(String message) {
        return error(NOT_FOUND, message);
    }

    /**
     * 快捷失败方法 返回500状态码 服务器内部错误
     * 
     * @param message 错误信息
     */
    public static <T> Result<T> serverError(String message) {
        return error(SERVER_ERROR, message);
    }

    // ------------ 链式方法 ------------

    /**
     * 链式方法，设置状态码
     * 
     * @param code 状态码
     * @return 当前结果对象
     */
    public Result<T> withCode(int code) {
        this.code = code;
        return this;
    }

    /**
     * 链式方法，设置消息
     * 
     * @param message 消息内容
     * @return 当前结果对象
     */
    public Result<T> withMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * 链式方法，设置数据
     * 
     * @param data 数据内容
     * @return 当前结果对象
     */
    public Result<T> withData(T data) {
        this.data = data;
        return this;
    }

}