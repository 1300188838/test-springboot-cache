package com.sanri.test.testcache.configs;

import com.sanri.test.testcache.configs.serializers.KryoRedisSerializer;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

/**
 * 缓存专用 RedisTemplate
 * key 使用 String 序列化
 * value 使用 kryo 序列化
 */
public class PrivateRedisTemplate extends RedisTemplate {

    public PrivateRedisTemplate(RedisConnectionFactory connectionFactory) {
        KryoRedisSerializer kryoRedisSerializer = new KryoRedisSerializer();
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        setKeySerializer(stringRedisSerializer);
        setHashKeySerializer(stringRedisSerializer);
        setValueSerializer(kryoRedisSerializer);
        setHashValueSerializer(kryoRedisSerializer);

        setConnectionFactory(connectionFactory);
        afterPropertiesSet();
    }
}
