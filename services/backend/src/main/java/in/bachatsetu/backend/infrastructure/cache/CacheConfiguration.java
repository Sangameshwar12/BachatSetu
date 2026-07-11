package in.bachatsetu.backend.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Generic Redis cache infrastructure: a named {@link CacheManager} with a distinct
 * time-to-live per cache region. No business module reads or writes through these caches
 * yet — this configuration only makes the infrastructure available for a future OTP
 * cache, rate limiter, session cache, or platform-configuration cache to opt into via
 * {@code @Cacheable(cacheNames = CacheConfiguration.OTP_CACHE)} or an injected
 * {@link CacheManager}, without any business module redesign in this sprint.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(prefix = "bachatsetu.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfiguration {

    public static final String OTP_CACHE = "otp";
    public static final String RATE_LIMIT_CACHE = "rate-limit";
    public static final String SESSION_CACHE = "session";
    public static final String CONFIG_CACHE = "config";

    @Bean
    CacheManager cacheManager(
            RedisConnectionFactory connectionFactory, CacheProperties properties, ObjectMapper objectMapper) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration(objectMapper, Duration.ofMinutes(5)))
                .withInitialCacheConfigurations(Map.of(
                        OTP_CACHE, cacheConfiguration(objectMapper, properties.otpTtl()),
                        RATE_LIMIT_CACHE, cacheConfiguration(objectMapper, properties.rateLimitTtl()),
                        SESSION_CACHE, cacheConfiguration(objectMapper, properties.sessionTtl()),
                        CONFIG_CACHE, cacheConfiguration(objectMapper, properties.configTtl())))
                .build();
    }

    private RedisCacheConfiguration cacheConfiguration(ObjectMapper objectMapper, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper)));
    }
}
