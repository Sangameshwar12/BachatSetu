package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.platformoperations.domain.model.SystemRuntimeInfo;
import org.junit.jupiter.api.Test;

class JvmSystemRuntimeInfoAdapterTest {

    @Test
    void readsRealJvmAndHostFacts() {
        JvmSystemRuntimeInfoAdapter adapter = new JvmSystemRuntimeInfoAdapter("1.0.0");

        SystemRuntimeInfo info = adapter.current();

        assertThat(info.applicationVersion()).isEqualTo("1.0.0");
        assertThat(info.buildTimestamp()).isNull();
        assertThat(info.javaVersion()).isNotBlank();
        assertThat(info.uptimeSeconds()).isGreaterThanOrEqualTo(0);
        assertThat(info.maxMemoryBytes()).isGreaterThan(0);
        assertThat(info.totalDiskBytes()).isGreaterThan(0);
    }
}
