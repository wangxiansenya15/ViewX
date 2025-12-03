package com.flowbrain.viewx.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Redis序列化工具类
 * 使用Jackson进行JSON序列化
 */
@Component
public class RedisSerializationUtils implements RedisSerializer<Object> {

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private final ObjectMapper objectMapper;

    public RedisSerializationUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(Object object) throws SerializationException {
        if (object == null) {
            return new byte[0];
        }

        try {
            // 对于基本类型和字符串直接处理
            if (object instanceof String) {
                return ((String) object).getBytes(DEFAULT_CHARSET);
            }
            if (object instanceof Number || object instanceof Boolean) {
                return object.toString().getBytes(DEFAULT_CHARSET);
            }

            return objectMapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new SerializationException("序列化对象失败: " + object, e);
        } catch (Exception e) {
            throw new SerializationException("序列化过程中发生错误", e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        return deserialize(bytes, Object.class);
    }

    /**
     * 反序列化为指定类型
     */
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            String str = new String(bytes, DEFAULT_CHARSET);

            // 如果是String类型直接返回
            if (clazz == String.class) {
                return (T) str;
            }

            // 处理基本类型
            if (clazz == Integer.class || clazz == int.class) {
                return (T) Integer.valueOf(str);
            }
            if (clazz == Long.class || clazz == long.class) {
                return (T) Long.valueOf(str);
            }
            if (clazz == Double.class || clazz == double.class) {
                return (T) Double.valueOf(str);
            }
            if (clazz == Float.class || clazz == float.class) {
                return (T) Float.valueOf(str);
            }
            if (clazz == Boolean.class || clazz == boolean.class) {
                return (T) Boolean.valueOf(str);
            }

            return objectMapper.readValue(bytes, clazz);
        } catch (Exception e) {
            throw new SerializationException("反序列化对象失败", e);
        }
    }

    /**
     * 反序列化为泛型类型
     */
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, JavaType javaType) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            return objectMapper.readValue(bytes, javaType);
        } catch (Exception e) {
            throw new SerializationException("反序列化对象失败", e);
        }
    }

    /**
     * 序列化对象为JSON字符串
     */
    public String serializeToJson(Object object) {
        if (object == null) {
            return null;
        }

        try {
            if (object instanceof String) {
                return (String) object;
            }
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new SerializationException("序列化对象为JSON失败", e);
        }
    }

    /**
     * 从JSON字符串反序列化对象
     */
    public <T> T deserializeFromJson(String json, Class<T> clazz) {
        if (!StringUtils.hasText(json)) {
            return null;
        }

        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new SerializationException("从JSON反序列化对象失败", e);
        }
    }

    /**
     * 创建集合类型的JavaType
     */
    public JavaType createCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
        return objectMapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }

    /**
     * 获取TypeFactory
     */
    public TypeFactory getTypeFactory() {
        return objectMapper.getTypeFactory();
    }
}