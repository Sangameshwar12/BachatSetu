package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.platformoperations.domain.model.ComponentHealth;
import in.bachatsetu.backend.platformoperations.domain.model.HealthStatus;
import org.junit.jupiter.api.Test;

class StorageHealthAdapterTest {

    @Test
    void reportsUpWhenEnabled() {
        StorageHealthAdapter adapter = new StorageHealthAdapter(true, "LOCAL");

        ComponentHealth health = adapter.check();

        assertThat(health.status()).isEqualTo(HealthStatus.UP);
        assertThat(health.detail()).contains("LOCAL");
    }

    @Test
    void reportsDownWhenDisabled() {
        StorageHealthAdapter adapter = new StorageHealthAdapter(false, "LOCAL");

        ComponentHealth health = adapter.check();

        assertThat(health.status()).isEqualTo(HealthStatus.DOWN);
    }
}
