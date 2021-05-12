package com.sanri.test.testcache.configs;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.SpringCacheAnnotationParser;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collection;

/**
 * 对于 cacheCustom 的解析器
 */
@Component
public class CacheCustomAnnotationParser {
    SpringCacheAnnotationParser annotationParser = new SpringCacheAnnotationParser();
    /**
     *
     * @param method
     * @return
     */
    public CacheCustomOperation parseCacheAnnotations(Method method) {
        CacheCustomOperation cacheCustomOperation = new CacheCustomOperation( method);

        Collection<CacheOperation> cacheOperations = annotationParser.parseCacheAnnotations(method);
        CacheableOperation cacheableOperation = null;
        for (CacheOperation cacheOperation : cacheOperations) {
            if(cacheOperation instanceof CacheableOperation){
                cacheableOperation = (CacheableOperation) cacheOperation;
            }
        }
        cacheCustomOperation.setCacheableOperation(cacheableOperation);

        CacheCustom cacheCustom = method.getAnnotation(CacheCustom.class);
        if(cacheCustom == null){
            //如果方法上没有配置,则取类上的配置
            Class<?> declaringClass = method.getDeclaringClass();
            cacheCustom = declaringClass.getAnnotation(CacheCustom.class);
            if(cacheCustom == null){
                return null;
            }
        }
        cacheCustomOperation.setThreshold(loadThreshold(cacheCustom));
        cacheCustomOperation.setExpire(Duration.parse(cacheCustom.expire()));
        cacheCustomOperation.setRedisSerializer(cacheCustom.valueSerializer());

        return cacheCustomOperation;
    }

    /**
     * 解析刷新时间
     * @param cacheCustom
     * @return
     */
    private Duration loadThreshold(CacheCustom cacheCustom) {
        String threshold = cacheCustom.threshold();
        if(StringUtils.isNotBlank(threshold)) {
            return  Duration.parse(threshold);
        }
        return null;
    }
}
