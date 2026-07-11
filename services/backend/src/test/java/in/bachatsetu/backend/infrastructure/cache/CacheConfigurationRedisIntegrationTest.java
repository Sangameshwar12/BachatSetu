package in.bachatsetu.backend.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Exercises {@link CacheConfiguration} against a real Redis server (Testcontainers), not a
 * full Spring context — the bean-building method is called directly, the same way this
 * codebase's Postgres persistence integration tests exercise a real database rather than a
 * mock. Skipped automatically when Docker isn't available, matching every other
 * Testcontainers-backed test in this codebase.
 */
@Testcontainers(disabledWithoutDocker = true)
class CacheConfigurationRedisIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        CacheProperties properties = new CacheProperties(
                true, Duration.ofMinutes(5), Duration.ofMinutes(1), Duration.ofMinutes(30), Duration.ofMinutes(10));
        cacheManager = new CacheConfiguration().cacheManager(connectionFactory, properties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void registersEveryNamedCacheRegionAgainstRealRedis() {
        assertThat(cacheManager.getCacheNames()).contains(
                CacheConfiguration.OTP_CACHE,
                CacheConfiguration.RATE_LIMIT_CACHE,
                CacheConfiguration.SESSION_CACHE,
                CacheConfiguration.CONFIG_CACHE);
    }

    @Test
    void aNamedCacheStoresAndRetrievesAValueThroughRealRedis() {
        Cache cache = cacheManager.getCache(CacheConfiguration.OTP_CACHE);
        assertThat(cache).isNotNull();

        cache.put("otp-test-key", "123456");

        assertThat(cache.get("otp-test-key", String.class)).isEqualTo("123456");
    }
}
