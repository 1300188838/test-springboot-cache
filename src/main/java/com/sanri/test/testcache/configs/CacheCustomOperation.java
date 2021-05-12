package com.sanri.test.testcache.configs;

import lombok.*;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class CacheCustomOperation {
    private CacheableOperation cacheableOperation;
    private Method method;
    private Duration expire;
    private Duration threshold;
    private Class<? extends RedisSerializer> redisSerializer;
    // md5 值的 redisKey ==> 执行的参数数组
    private Map<String,Object[]> invokeArgsMap = new HashMap<>();

    public CacheCustomOperation(Method method) {
        this.method = method;
    }
}
