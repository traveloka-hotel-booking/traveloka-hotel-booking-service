package com.re.travelokahotelbookingservice.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    /**
     * Cấu hình RedisTemplate dùng chung cho việc lưu trữ Key-Value tự do.
     * Sử dụng StringSerializer cho Key và Jackson2JsonSerializer cho Value.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer serializer = createJsonSerializer();

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Cấu hình CacheManager đa tầng (L1 In-Memory RAM + L2 Redis) kèm Circuit Breaker:
     * - Phản hồi tức thì (< 1ms) từ bộ nhớ RAM L1 cho các truy vấn trang chủ / danh sách khách sạn.
     * - Khi Redis khả dụng: đồng bộ dữ liệu giữa L1 và L2 Redis.
     * - Khi Redis chưa khởi chạy hoặc mất kết nối (ví dụ môi trường Cloud Free Tier): tự động mở
     *   Circuit Breaker trong 30 giây để không bị treo 2-4 giây chờ timeout Redis, đồng thời vẫn
     *   lưu và phục vụ cache trực tiếp từ RAM L1 thay vì phải truy vấn lại MySQL liên tục.
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = createJsonSerializer();

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        java.util.Map<String, RedisCacheConfiguration> initialCacheConfig = new java.util.HashMap<>();
        initialCacheConfig.put("search_hotels", cacheConfig.entryTtl(Duration.ofMinutes(5)));
        initialCacheConfig.put("filter_hotels", cacheConfig.entryTtl(Duration.ofMinutes(5)));
        initialCacheConfig.put("hotels_list", cacheConfig.entryTtl(Duration.ofMinutes(10)));
        initialCacheConfig.put("hotel_detail", cacheConfig.entryTtl(Duration.ofMinutes(10)));
        initialCacheConfig.put("hotel_rooms", cacheConfig.entryTtl(Duration.ofMinutes(10)));
        initialCacheConfig.put("user_bookings", cacheConfig.entryTtl(Duration.ofMinutes(3)));
        initialCacheConfig.put("user_profile", cacheConfig.entryTtl(Duration.ofMinutes(15)));

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .withInitialCacheConfigurations(initialCacheConfig)
                .build();
        redisCacheManager.afterPropertiesSet();

        Map<String, Long> ttlByCacheName = Map.of(
                "search_hotels", Duration.ofMinutes(5).toMillis(),
                "filter_hotels", Duration.ofMinutes(5).toMillis(),
                "hotels_list", Duration.ofMinutes(10).toMillis(),
                "hotel_detail", Duration.ofMinutes(10).toMillis(),
                "hotel_rooms", Duration.ofMinutes(10).toMillis(),
                "user_bookings", Duration.ofMinutes(3).toMillis(),
                "user_profile", Duration.ofMinutes(15).toMillis()
        );

        return new ResilientTieredCacheManager(redisCacheManager, ttlByCacheName, Duration.ofMinutes(10).toMillis());
    }

    /**
     * CacheManager kết hợp L1 (ConcurrentHashMap có TTL) và L2 (RedisCacheManager) với Circuit Breaker.
     */
    static class ResilientTieredCacheManager implements CacheManager {
        private final RedisCacheManager redisCacheManager;
        private final Map<String, Long> ttlByCacheName;
        private final long defaultTtlMs;
        private final ConcurrentHashMap<String, Cache> caches = new ConcurrentHashMap<>();
        private final AtomicLong redisCircuitOpenUntil = new AtomicLong(0L);

        ResilientTieredCacheManager(RedisCacheManager redisCacheManager, Map<String, Long> ttlByCacheName, long defaultTtlMs) {
            this.redisCacheManager = redisCacheManager;
            this.ttlByCacheName = ttlByCacheName;
            this.defaultTtlMs = defaultTtlMs;
        }

        @Override
        public Cache getCache(String name) {
            return caches.computeIfAbsent(name, cacheName -> {
                Cache redisDelegate = null;
                try {
                    redisDelegate = redisCacheManager.getCache(cacheName);
                } catch (Exception e) {
                    log.warn("Could not obtain Redis cache delegate for '{}': {}", cacheName, e.getMessage());
                }
                long ttlMs = ttlByCacheName.getOrDefault(cacheName, defaultTtlMs);
                return new ResilientTieredCache(cacheName, redisDelegate, ttlMs, redisCircuitOpenUntil);
            });
        }

        @Override
        public Collection<String> getCacheNames() {
            return caches.keySet();
        }
    }

    /**
     * Cache đa tầng L1 (RAM) + L2 (Redis) giúp loại bỏ độ trễ khi khởi động hoặc khi Redis ngủ đông.
     */
    static class ResilientTieredCache implements Cache {
        private static final long CIRCUIT_BREAKER_COOLDOWN_MS = 30_000L;
        private static final int MAX_L1_ENTRIES = 500;

        private final String name;
        private final Cache redisDelegate;
        private final long ttlMs;
        private final AtomicLong redisCircuitOpenUntil;
        private final ConcurrentHashMap<Object, LocalCacheEntry> l1Store = new ConcurrentHashMap<>();

        record LocalCacheEntry(Object value, long expiresAt) {
            boolean isExpired() {
                return System.currentTimeMillis() > expiresAt;
            }
        }

        ResilientTieredCache(String name, Cache redisDelegate, long ttlMs, AtomicLong redisCircuitOpenUntil) {
            this.name = name;
            this.redisDelegate = redisDelegate;
            this.ttlMs = ttlMs;
            this.redisCircuitOpenUntil = redisCircuitOpenUntil;
        }

        private boolean isRedisAvailable() {
            return redisDelegate != null && System.currentTimeMillis() >= redisCircuitOpenUntil.get();
        }

        private void tripCircuitBreaker(RuntimeException ex, String op) {
            long now = System.currentTimeMillis();
            long prev = redisCircuitOpenUntil.getAndSet(now + CIRCUIT_BREAKER_COOLDOWN_MS);
            if (now >= prev) {
                log.warn("Redis {} error on cache '{}': {}. Using L1 in-memory cache for next 30s.",
                        op, name, ex.getMessage());
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return l1Store;
        }

        @Override
        public ValueWrapper get(Object key) {
            LocalCacheEntry local = l1Store.get(key);
            if (local != null) {
                if (!local.isExpired()) {
                    return new SimpleValueWrapper(local.value());
                }
                l1Store.remove(key, local);
            }

            if (isRedisAvailable()) {
                try {
                    ValueWrapper redisVal = redisDelegate.get(key);
                    if (redisVal != null && redisVal.get() != null) {
                        putInL1(key, redisVal.get());
                        return redisVal;
                    }
                } catch (RuntimeException ex) {
                    tripCircuitBreaker(ex, "GET");
                }
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Class<T> type) {
            ValueWrapper wrapper = get(key);
            return wrapper != null ? (T) wrapper.get() : null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Callable<T> valueLoader) {
            ValueWrapper existing = get(key);
            if (existing != null) {
                return (T) existing.get();
            }
            try {
                T value = valueLoader.call();
                put(key, value);
                return value;
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
        }

        @Override
        public void put(Object key, Object value) {
            if (value == null) {
                return;
            }
            putInL1(key, value);
            if (isRedisAvailable()) {
                try {
                    redisDelegate.put(key, value);
                } catch (RuntimeException ex) {
                    tripCircuitBreaker(ex, "PUT");
                }
            }
        }

        private void putInL1(Object key, Object value) {
            if (l1Store.size() >= MAX_L1_ENTRIES) {
                long now = System.currentTimeMillis();
                l1Store.entrySet().removeIf(e -> e.getValue().expiresAt() <= now);
                if (l1Store.size() >= MAX_L1_ENTRIES) {
                    l1Store.clear();
                }
            }
            l1Store.put(key, new LocalCacheEntry(value, System.currentTimeMillis() + ttlMs));
        }

        @Override
        public void evict(Object key) {
            l1Store.remove(key);
            if (isRedisAvailable()) {
                try {
                    redisDelegate.evict(key);
                } catch (RuntimeException ex) {
                    tripCircuitBreaker(ex, "EVICT");
                }
            }
        }

        @Override
        public void clear() {
            l1Store.clear();
            if (isRedisAvailable()) {
                try {
                    redisDelegate.clear();
                } catch (RuntimeException ex) {
                    tripCircuitBreaker(ex, "CLEAR");
                }
            }
        }
    }

    /**
     * Cấu hình Jackson ObjectMapper hỗ trợ LocalDate, LocalDateTime và polymorphic typing.
     */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    /**
     * Cơ chế Graceful Degradation (Khả năng chịu lỗi an toàn):
     * Nếu Redis bị tắt hoặc mất kết nối, hệ thống sẽ log cảnh báo và tự động
     * fallback đọc/ghi trực tiếp vào MySQL, không làm sập ứng dụng hoặc lỗi API của người dùng!
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis GET error on cache '{}' for key '{}': {}. Fallback to DB.", 
                        cache != null ? cache.getName() : "unknown", key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis PUT error on cache '{}' for key '{}': {}", 
                        cache != null ? cache.getName() : "unknown", key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis EVICT error on cache '{}' for key '{}': {}", 
                        cache != null ? cache.getName() : "unknown", key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis CLEAR error on cache '{}': {}", 
                        cache != null ? cache.getName() : "unknown", exception.getMessage());
            }
        };
    }
}
