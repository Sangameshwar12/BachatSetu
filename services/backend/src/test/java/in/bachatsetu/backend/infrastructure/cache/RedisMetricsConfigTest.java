package in.bachatsetu.backend.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;

import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.resource.ClientResources;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.data.redis.ClientResourcesBuilderCustomizer;

class RedisMetricsConfigTest {

    @Test
    void customizesClientResourcesWithAMicrometerCommandLatencyRecorder() {
        ClientResourcesBuilderCustomizer customizer = new RedisMetricsConfig()
                .redisCommandLatencyMetricsCustomizer(new SimpleMeterRegistry());
        ClientResources.Builder builder = ClientResources.builder();

        customizer.customize(builder);
        ClientResources resources = builder.build();
        try {
            assertThat(resources.commandLatencyRecorder()).isInstanceOf(MicrometerCommandLatencyRecorder.class);
        } finally {
            resources.shutdown();
        }
    }
}
