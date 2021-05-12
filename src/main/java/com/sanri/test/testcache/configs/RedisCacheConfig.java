package com.sanri.test.testcache.configs;

import com.sanri.test.testcache.configs.serializers.KryoRedisSerializer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
public class RedisCacheConfig {

	@Bean
	public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
		RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);
		RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();
		RedisCacheConfiguration customRedisCacheConfiguration = redisCacheConfiguration.entryTtl(Duration.ofMinutes(1))
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new KryoRedisSerializer()));
		CustomRedisCacheManager customRedisCacheManager = new CustomRedisCacheManager(cacheWriter, customRedisCacheConfiguration);
		// 空值 5 分钟失效
		customRedisCacheManager.setEmptyKeyExpire(Duration.ofMinutes(5));
		// 配置 people 10 秒失效
		customRedisCacheManager.addExpire("people",Duration.ofSeconds(10));
		return customRedisCacheManager;
	}

	@Bean
	public KeyGenerator keyGenerator(){
		return new RedisCacheKeyGenerator();
	}

	@Bean
	public PrivateRedisTemplate privateRedisTemplate(RedisConnectionFactory redisConnectionFactory){
		return new PrivateRedisTemplate(redisConnectionFactory);
	}
}
