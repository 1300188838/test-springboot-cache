package com.sanri.test.testcache.configs;

import com.sanri.test.testcache.configs.serializers.KryoRedisSerializer;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.data.redis.connection.FutureResult;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;

@Aspect
@Component
public class CacheCustomAspect {
    @Autowired
    private KeyGenerator keyGenerator;

    @Pointcut("@annotation(com.sanri.test.testcache.configs.CacheCustom)")
    public void pointCut(){}

    public static final String INVOCATION_CACHE_KEY_SUFFIX = ":invocation_cache_key_suffix";

    @Autowired
    private RedisTemplate redisTemplate;

    @Before("pointCut()")
    public void registerInvoke(JoinPoint joinPoint){
        Object[] args = joinPoint.getArgs();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Object target = joinPoint.getTarget();

        Object cacheKey = keyGenerator.generate(target, method, args);
        String methodInvokeKey = cacheKey + INVOCATION_CACHE_KEY_SUFFIX;
        if(redisTemplate.hasKey(methodInvokeKey)){
            return ;
        }

        // 将方法执行器写入 redis ,然后需要刷新的时候从 redis 获取执行器,根据 cacheKey ,然后刷新缓存
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
        MethodInvoker methodInvoker = new MethodInvoker();
        methodInvoker.setTargetClass(targetClass);
        methodInvoker.setTargetMethod(method.getName());
        methodInvoker.setArguments(args);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new KryoRedisSerializer());
        redisTemplate.opsForValue().set(methodInvokeKey,methodInvoker);
    }


}
