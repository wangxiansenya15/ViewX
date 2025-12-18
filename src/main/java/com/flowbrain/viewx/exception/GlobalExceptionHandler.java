package com.flowbrain.viewx.exception;

import com.flowbrain.viewx.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.flowbrain.viewx.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.file.AccessDeniedException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 检查当前请求是否为 Swagger 相关路径
     * 对于 Swagger 文档生成请求，不应该被全局异常处理器拦截
     */
    private boolean isSwaggerRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String requestURI = request.getRequestURI();
            return requestURI.startsWith("/v3/api-docs") ||
                    requestURI.startsWith("/swagger-ui") ||
                    requestURI.startsWith("/swagger-resources") ||
                    requestURI.contains("api-docs");
        }
        return false;
    }

    // 业务异常基类处理
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }

    // 资源不存在异常 (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    Result<?> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("资源不存在: {}", ex.getMessage());
        return Result.notFound(ex.getMessage());
    }

    // 参数校验异常 (400)
    @ExceptionHandler(ValidationException.class)
    public Result<?> handleValidationException(ValidationException ex) {
        log.warn("参数校验失败: {}", ex.getMessage());
        return Result.badRequest(ex.getMessage());
    }

    // 认证异常 (401)
    @ExceptionHandler(AuthenticationException.class)
    public Result<?> handleAuthenticationException(AuthenticationException ex) {
        log.warn("认证失败: {}", ex.getMessage());
        return Result.unauthorized(ex.getMessage());
    }

    // 权限不足异常 (403)
    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("权限不足: {}", ex.getMessage());
        return Result.forbidden(ex.getMessage());
    }

    // 冲突异常 (409)
    @ExceptionHandler(ConflictException.class)
    public Result<?> handleConflictException(ConflictException ex) {
        log.warn("操作冲突: {}", ex.getMessage());
        return Result.error(409, ex.getMessage());
    }

    // 数据库异常 (500)
    @ExceptionHandler(DatabaseException.class)
    public Result<?> handleDatabaseException(DatabaseException ex) {
        log.error("数据库异常: {}", ex.getMessage(), ex);
        return Result.serverError("数据操作失败，请稍后重试");
    }

    // 外部服务异常 (503)
    @ExceptionHandler(ExternalServiceException.class)
    public Result<?> handleExternalServiceException(ExternalServiceException ex) {
        log.error("外部服务调用失败: {}", ex.getMessage(), ex);
        return Result.error(503, "服务暂时不可用，请稍后重试");
    }

    // SQL 语法错误异常 (500)
    @ExceptionHandler(org.springframework.jdbc.BadSqlGrammarException.class)
    public Result<?> handleBadSqlGrammarException(org.springframework.jdbc.BadSqlGrammarException ex) {
        log.error("SQL 语法错误: {}", ex.getMessage(), ex);

        // 提取具体的 SQL 错误信息
        String errorMsg = "数据库查询错误";
        if (ex.getCause() != null) {
            String causeMsg = ex.getCause().getMessage();
            if (causeMsg != null) {
                // 提取关键错误信息
                if (causeMsg.contains("column") && causeMsg.contains("does not exist")) {
                    errorMsg = "数据库字段不存在，请检查 SQL 查询";
                } else if (causeMsg.contains("syntax error")) {
                    errorMsg = "SQL 语法错误";
                } else if (causeMsg.contains("relation") && causeMsg.contains("does not exist")) {
                    errorMsg = "数据库表不存在";
                }
            }
        }

        return Result.serverError(errorMsg);
    }

    // MyBatis 异常 (500)
    @ExceptionHandler(org.mybatis.spring.MyBatisSystemException.class)
    public Result<?> handleMyBatisSystemException(org.mybatis.spring.MyBatisSystemException ex) {
        log.error("MyBatis 系统异常: {}", ex.getMessage(), ex);
        return Result.serverError("数据访问层异常，请联系管理员");
    }

    // 数据完整性违反异常 (409)
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public Result<?> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        log.error("数据完整性违反: {}", ex.getMessage(), ex);

        String errorMsg = "数据操作违反约束";
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("duplicate key") || ex.getMessage().contains("unique constraint")) {
                errorMsg = "数据已存在，违反唯一性约束";
            } else if (ex.getMessage().contains("foreign key")) {
                errorMsg = "违反外键约束，关联数据不存在";
            }
        }

        return Result.error(409, errorMsg);
    }

    // 404 - 端点不存在异常
    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public Result<?> handleNoHandlerFound(org.springframework.web.servlet.NoHandlerFoundException ex) {
        log.warn("请求的端点不存在: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return Result.error(404, "请求的接口不存在: " + ex.getHttpMethod() + " " + ex.getRequestURL());
    }

    // 全局异常兜底处理 (500)
    @ExceptionHandler(Exception.class)
    public Result<?> handleGlobalException(Exception ex) {
        // 如果是 Swagger 相关请求，不拦截异常，让其正常抛出
        if (isSwaggerRequest()) {
            throw new RuntimeException(ex);
        }

        log.error("系统未知异常: ", ex);
        return Result.serverError("系统繁忙，请稍后重试");
    }
}