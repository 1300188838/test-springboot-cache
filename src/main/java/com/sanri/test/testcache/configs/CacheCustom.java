package com.sanri.test.testcache.configs;

import com.sanri.test.testcache.configs.serializers.KryoRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.lang.annotation.*;

/**
 * 自定义缓存
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})
public @interface CacheCustom {
    /**
     * 缓存失效时间
     * 使用 ISO-8601持续时间格式
     * Examples:
     *   <pre>
     *      "PT20.345S" -- parses as "20.345 seconds"
     *      "PT15M"     -- parses as "15 minutes" (where a minute is 60 seconds)
     *      "PT10H"     -- parses as "10 hours" (where an hour is 3600 seconds)
     *      "P2D"       -- parses as "2 days" (where a day is 24 hours or 86400 seconds)
     *      "P2DT3H4M"  -- parses as "2 days, 3 hours and 4 minutes"
     *      "P-6H3M"    -- parses as "-6 hours and +3 minutes"
     *      "-P6H3M"    -- parses as "-6 hours and -3 minutes"
     *      "-P-6H+3M"  -- parses as "+6 hours and -3 minutes"
     *   </pre>
     * @return
     */
    String expire() default "PT60s";

    /**
     * 刷新时间阀值，不配置将不会进行缓存刷新
     * 对于像前端的分页条件查询，建议不配置，这将在内存生成一个执行映射，太多的话将会占用太多的内存使用空间
     * 此功能适用于像字典那种需要定时刷新缓存的功能
     * @return
     */
    String threshold() default "";

    /**
     * 值的序列化方式
     * @return
     */
    Class<? extends RedisSerializer> valueSerializer() default KryoRedisSerializer.class;
}
