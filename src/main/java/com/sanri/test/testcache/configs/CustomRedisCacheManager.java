package com.sanri.test.testcache.configs;

import com.sanri.test.testcache.configs.serializers.KryoRedisSerializer;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;

/**
 * 订制缓存管理，增强 redis 缓存配置
 * 1. 解决缓存预热(暂未解决)
 * 2. 解决缓存有效时间
 * 3. 解决缓存刷新问题
 * 4. 增加空值缓存配置存储时间,不能存储大量的空键,需要清理
 */
public class CustomRedisCacheManager extends RedisCacheManager implements ApplicationContextAware, InitializingBean {
	// 其它缓存键属性配置
	private Map<String, RedisCacheConfiguration> initialCacheConfiguration = new LinkedHashMap<>();
	// cacheName == > CacheCustomOperation
	private Map<String,CacheCustomOperation> cacheCustomOperationMap = new HashMap<>();
	// 简易配置过期时间 cacheName ==> 过期时间
	private Map<String,Duration> simpleConfigExpire = new HashMap<>();

	// 空键存储时间
	private Duration emptyKeyExpire;

	private ApplicationContext applicationContext;

	private RedisCacheWriter cacheWriter;
	RedisCacheConfiguration defaultCacheConfiguration;
	@Autowired
	private PrivateRedisTemplate redisTemplate;

	public CustomRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration) {
		super(cacheWriter, defaultCacheConfiguration);
		this.cacheWriter = cacheWriter;
		this.defaultCacheConfiguration = defaultCacheConfiguration;
	}
	@Autowired
	private CacheCustomAnnotationParser cacheCustomAnnotationParser;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * 重写  loadCaches ，使用本类的 initialCacheConfiguration
	 * @return
	 */
	@Override
	protected Collection<RedisCache> loadCaches() {
		List<RedisCache> caches = new LinkedList<>();

		for (Map.Entry<String, RedisCacheConfiguration> entry : initialCacheConfiguration.entrySet()) {
			caches.add(createRedisCache(entry.getKey(), entry.getValue()));
		}

		//再把父类的一并加载了，不然配置类里配置的会不生效
		Collection<RedisCache> redisCaches = super.loadCaches();
		caches.addAll(redisCaches);
		return caches;
	}

    @Override
	public void afterPropertiesSet() {
		//配置其它个性化缓存配置（查找顶层类配置为CacheConfig | CacheCustom ）
		Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(CacheCustom.class);
		Map<String, Object> cacheConfigAnnotation = applicationContext.getBeansWithAnnotation(CacheConfig.class);
		beansWithAnnotation.putAll(cacheConfigAnnotation);

		// 用于排重，相同配置不需要配置多份 key 为 expire + threshold + serializer
		Map<String,RedisCacheConfiguration> cachedRedisConfiguration = new HashMap<>();

		Map<String,RedisCacheConfiguration> redisCacheConfigurationMap = new HashMap<>();
		for (Object target : beansWithAnnotation.values()) {
			Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
			CacheCustom cacheCustom = targetClass.getAnnotation(CacheCustom.class);
			if(cacheCustom != null){
				List<Method> methodsListWithAnnotation = MethodUtils.getMethodsListWithAnnotation(targetClass, CacheCustom.class, true, true);
				for (Method method : methodsListWithAnnotation) {
					CacheCustomOperation cacheCustomOperation = cacheCustomAnnotationParser.parseCacheAnnotations(method);
					// 存储 cacheName ==> CacheCustomOperation 供刷新缓存时使用
					Set<String> cacheNames = cacheCustomOperation.getCacheableOperation().getCacheNames();
					Iterator<String> iterator = cacheNames.iterator();
					while (iterator.hasNext()){
						String cacheName = iterator.next();
						cacheCustomOperationMap.put(cacheName,cacheCustomOperation);
					}

					//配置个性化的初始化缓存,针对每个 cacheName 一份,对配置信息做一份缓存,相同的配置只取一份
					Duration expire = cacheCustomOperation.getExpire();
					Duration threshold = cacheCustomOperation.getThreshold();
					String thresholdString = threshold == null ? "":threshold.toString();
					Class<? extends RedisSerializer> redisSerializer = cacheCustomOperation.getRedisSerializer();
					RedisCacheConfiguration redisCacheConfiguration = getRedisCacheConfiguration(cachedRedisConfiguration, expire, thresholdString, redisSerializer);

					// 配置默认过期策略
					Iterator<String> iterator1 = cacheNames.iterator();
					while (iterator1.hasNext()){
						String cacheName = iterator1.next();
						initialCacheConfiguration.put(cacheName,redisCacheConfiguration);
					}

				}

			}
		}

		// 对于简易配置的 cacheKey 配置过期策略; 这种的序列化方法默认使用 Kryo 序列化
		simpleConfigExpire.forEach((cacheName,expire) -> {
			RedisCacheConfiguration redisCacheConfiguration = getRedisCacheConfiguration(cachedRedisConfiguration, expire, "", KryoRedisSerializer.class);
			initialCacheConfiguration.put(cacheName,redisCacheConfiguration);
		});

		super.afterPropertiesSet();
	}

	private RedisCacheConfiguration getRedisCacheConfiguration(Map<String, RedisCacheConfiguration> cachedRedisConfiguration, Duration expire, String thresholdString, Class<? extends RedisSerializer> redisSerializer) {
		String key = expire.toString() + thresholdString + redisSerializer;
		RedisCacheConfiguration redisCacheConfiguration = cachedRedisConfiguration.get(key);
		if(redisCacheConfiguration == null){
			RedisSerializer redisSerializerInstance = (RedisSerializer) ReflectUtils.newInstance(redisSerializer);
			redisCacheConfiguration = redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
					.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializerInstance))
//								.disableKeyPrefix()
					.entryTtl(expire);
			cachedRedisConfiguration.put(key,redisCacheConfiguration);
		}
		return redisCacheConfiguration;
	}

	@Override
	public Cache getCache(String cacheName) {
		CacheCustomOperation cacheCustomOperation = cacheCustomOperationMap.get(cacheName);
		RedisCacheConfiguration redisCacheConfiguration = initialCacheConfiguration.get(cacheName);
		if(redisCacheConfiguration == null){redisCacheConfiguration = defaultCacheConfiguration;}

		CustomRedisCache customRedisCache = new CustomRedisCache(cacheName,cacheWriter,redisCacheConfiguration, redisTemplate, applicationContext, cacheCustomOperation);
		customRedisCache.setEmptyKeyExpire(this.emptyKeyExpire);
		return customRedisCache;
	}

	public void setEmptyKeyExpire(Duration emptyKeyExpire) {
		this.emptyKeyExpire = emptyKeyExpire;
	}

	/**
	 * 添加过期时间
	 * @param cacheName
	 * @param expire
	 */
	public void addExpire(String cacheName,Duration expire){
		simpleConfigExpire.put(cacheName,expire);
	}
}
