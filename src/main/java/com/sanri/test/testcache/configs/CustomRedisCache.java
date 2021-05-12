package com.sanri.test.testcache.configs;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MethodInvoker;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CustomRedisCache extends RedisCache {
    private RedisTemplate redisTemplate;
    private ApplicationContext applicationContext;
    private CacheCustomOperation cacheCustomOperation;
    private Duration emptyKeyExpire;

    public CustomRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig, RedisTemplate redisTemplate, ApplicationContext applicationContext, CacheCustomOperation cacheCustomOperation) {
        super(name, cacheWriter, cacheConfig);
        this.redisTemplate = redisTemplate;
        this.applicationContext = applicationContext;
        this.cacheCustomOperation = cacheCustomOperation;
    }

    @Override
    public ValueWrapper get(Object cacheKey) {
        if(cacheCustomOperation == null){return super.get(cacheKey);}

        Duration threshold = cacheCustomOperation.getThreshold();
        if(threshold == null){
            // 如果不需要刷新,直接取值
            return super.get(cacheKey);
        }

        //判断是否需要刷新
        Long expire = redisTemplate.getExpire(cacheKey);
        if(expire != -2 && expire < threshold.getSeconds()){
            log.info("当前剩余过期时间["+expire+"]小于刷新阀值["+threshold.getSeconds()+"],刷新缓存:"+cacheKey+",在 cacheNmae为 :"+this.getName());
            synchronized (CustomRedisCache.class) {
                refreshCache(cacheKey.toString(), threshold);
            }
        }

        return super.get(cacheKey);
    }

    @Override
    public void put(Object key, Object value) {
       super.put(key,value);
       if(emptyKeyExpire == null){
           return ;
       }

       //如果值为空,设置过期时间
       if(value != null){
           if(value instanceof Collection){
               Collection collection = (Collection) value;
               if(!CollectionUtils.isEmpty(collection)){return ;}
           }else if(value instanceof Map){
               Map map = (Map) value;
               if(!map.isEmpty()){return ;}
           }else if(value.getClass().isArray() ){
               if(ArrayUtils.isNotEmpty((Object [])value)){return ;}
           }else if(value instanceof String){
               if(StringUtils.isNotEmpty((CharSequence) value)){return ;}
           }
       }
       // 设置空值过期时间
        String cacheKey = createCacheKey(key);
        redisTemplate.expire(cacheKey,emptyKeyExpire.toMillis(),TimeUnit.MILLISECONDS);
    }

    /**
     * 刷新缓存
     * @param cacheKey
     * @param threshold
     * @return
     */
    private void refreshCache(String cacheKey, Duration threshold) {
        String methodInvokeKey = cacheKey + CacheCustomAspect.INVOCATION_CACHE_KEY_SUFFIX;
        MethodInvoker methodInvoker = (MethodInvoker) redisTemplate.opsForValue().get(methodInvokeKey);
        if(methodInvoker != null){
            Class<?> targetClass = methodInvoker.getTargetClass();
            Object target = AopProxyUtils.getSingletonTarget(applicationContext.getBean(targetClass));
//            Object target = getTarget(applicationContext.getBean(targetClass));
            methodInvoker.setTargetObject(target);
            try {
                methodInvoker.prepare();
                Object invoke = methodInvoker.invoke();

                //然后设置进缓存和重新设置过期时间
                this.put(cacheKey,invoke);
                long ttl = threshold.toMillis();
                redisTemplate.expire(cacheKey,ttl, TimeUnit.MILLISECONDS);
            } catch (InvocationTargetException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException e) {
                log.error("刷新缓存失败:"+e.getMessage(),e);
            }

        }
    }

    public void setEmptyKeyExpire(Duration emptyKeyExpire) {
        this.emptyKeyExpire = emptyKeyExpire;
    }
}
