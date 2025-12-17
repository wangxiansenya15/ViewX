package com.flowbrain.viewx.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * 自定义JSON Redis序列化器
 * 用于Redis数据的序列化和反序列化，支持Java对象与JSON之间的转换
 * 增强了对Java 8日期时间API的支持，并提供了更多的配置选项
 * 
 * @author Arthur Wang
 * @date 2025/5/17
 */
public class CustomJsonRedisSerializer<T> implements RedisSerializer<T> {
    private static final Logger logger = LoggerFactory.getLogger(CustomJsonRedisSerializer.class);

    /**
     * -- GETTER --
     *  获取配置的ObjectMapper实例
     *
     */
    @Getter
    private final ObjectMapper objectMapper;
    private final Class<T> type;

    /**
     * 创建一个默认配置的序列化器
     * 
     * @param type 要序列化/反序列化的类型
     */
    public CustomJsonRedisSerializer(Class<T> type) {
        this(type, true);
    }
    
    /**
     * 创建一个可配置的序列化器
     * 
     * @param type 要序列化/反序列化的类型
     * @param enableDefaultTyping 是否启用默认类型信息
     */
    public CustomJsonRedisSerializer(Class<T> type, boolean enableDefaultTyping) {
        this.type = type;
        this.objectMapper = createObjectMapper(enableDefaultTyping);
    }
    
    /**
     * 创建并配置ObjectMapper
     * 
     * @param enableDefaultTyping 是否启用默认类型信息
     * @return 配置好的ObjectMapper实例
     */
    private ObjectMapper createObjectMapper(boolean enableDefaultTyping) {
        ObjectMapper mapper = new ObjectMapper();
        
        // 配置可见性规则
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        // 添加Java 8日期时间模块支持
        mapper.registerModule(new JavaTimeModule());
        
        // 禁用日期时间作为时间戳输出
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 忽略未知属性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // 启用自动类型识别（如果需要）
        if (enableDefaultTyping) {
            mapper.activateDefaultTyping(
                    LaissezFaireSubTypeValidator.instance,
                    ObjectMapper.DefaultTyping.NON_FINAL,
                    JsonTypeInfo.As.PROPERTY
            );
        }
        
        return mapper;
    }

    @Override
    public byte[] serialize(T object) throws SerializationException {
        if (object == null) {
            return new byte[0];
        }
        
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (Exception e) {
            logger.error("Redis序列化对象失败: {}", e.getMessage());
            throw new SerializationException("序列化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        
        try {
            return objectMapper.readValue(bytes, type);
        } catch (Exception e) {
            logger.error("Redis反序列化对象失败: {}", e.getMessage());
            throw new SerializationException("反序列化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将对象序列化为JSON字符串
     * 
     * @param object 要序列化的对象
     * @return JSON字符串
     */
    public String toJsonString(T object) {
        if (object == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            logger.error("对象转JSON字符串失败: {}", e.getMessage());
            throw new SerializationException("对象转JSON字符串失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将JSON字符串反序列化为对象
     * 
     * @param json JSON字符串
     * @return 反序列化后的对象
     */
    public T fromJsonString(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            logger.error("JSON字符串转对象失败: {}", e.getMessage());
            throw new SerializationException("JSON字符串转对象失败: " + e.getMessage(), e);
        }
    }
}
