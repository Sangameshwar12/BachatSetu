package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.NotificationSpringDataRepository;
import in.bachatsetu.backend.platformoperations.domain.model.ComponentHealth;
import in.bachatsetu.backend.platformoperations.domain.model.HealthStatus;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class NotificationHealthAdapterTest {

    @Test
    void reportsUpWhenTheRepositoryIsReachable() {
        NotificationSpringDataRepository repository = mock(NotificationSpringDataRepository.class);
        when(repository.countByDeletedFalse()).thenReturn(5L);
        NotificationHealthAdapter adapter = new NotificationHealthAdapter(repository);

        ComponentHealth health = adapter.check();

        assertThat(health.status()).isEqualTo(HealthStatus.UP);
    }

    @Test
    void reportsDownWhenTheRepositoryFails() {
        NotificationSpringDataRepository repository = mock(NotificationSpringDataRepository.class);
        when(repository.countByDeletedFalse()).thenThrow(new DataAccessResourceFailureException("down"));
        NotificationHealthAdapter adapter = new NotificationHealthAdapter(repository);

        ComponentHealth health = adapter.check();

        assertThat(health.status()).isEqualTo(HealthStatus.DOWN);
    }
}
