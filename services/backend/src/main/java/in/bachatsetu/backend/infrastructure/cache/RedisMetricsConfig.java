package in.bachatsetu.backend.infrastructure.cache;

import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.metrics.MicrometerOptions;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.data.redis.ClientResourcesBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Records Lettuce (Redis client) command latency into the application's {@link MeterRegistry}
 * so it is exported alongside every other metric at {@code /actuator/prometheus} — see
 * docs/deployment/monitoring-guide.md. Spring Boot auto-configures the {@link
 * io.lettuce.core.resource.ClientResources} used by the Redis connection factory; this bean only
 * customizes that auto-configured instance, it does not replace or reconfigure the connection
 * itself.
 */
@Configuration(proxyBeanMethods = false)
public class RedisMetricsConfig {

    @Bean
    ClientResourcesBuilderCustomizer redisCommandLatencyMetricsCustomizer(MeterRegistry meterRegistry) {
        return builder -> builder.commandLatencyRecorder(
                new MicrometerCommandLatencyRecorder(meterRegistry, MicrometerOptions.create()));
    }
}
