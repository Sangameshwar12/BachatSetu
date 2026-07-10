package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.domain.configuration.model.LimitKey;
import in.bachatsetu.backend.admin.domain.configuration.model.PlatformLimit;
import in.bachatsetu.backend.infrastructure.persistence.entity.config.PlatformLimitJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PlatformLimitSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlatformLimitRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    @Test
    void findsAllLimits() {
        PlatformLimitSpringDataRepository repository = mock(PlatformLimitSpringDataRepository.class);
        when(repository.findAll()).thenReturn(
                List.of(new PlatformLimitJpaEntity(LimitKey.MAX_GROUPS, 500, 0, NOW, null)));
        PlatformLimitRepositoryAdapter adapter = new PlatformLimitRepositoryAdapter(repository);

        List<PlatformLimit> limits = adapter.findAll();

        assertThat(limits).hasSize(1);
        assertThat(limits.get(0).key()).isEqualTo(LimitKey.MAX_GROUPS);
    }

    @Test
    void findsByKey() {
        PlatformLimitSpringDataRepository repository = mock(PlatformLimitSpringDataRepository.class);
        when(repository.findById(LimitKey.MAX_MEMBERS)).thenReturn(
                Optional.of(new PlatformLimitJpaEntity(LimitKey.MAX_MEMBERS, 1000, 1, NOW, null)));
        PlatformLimitRepositoryAdapter adapter = new PlatformLimitRepositoryAdapter(repository);

        Optional<PlatformLimit> limit = adapter.findByKey(LimitKey.MAX_MEMBERS);

        assertThat(limit).isPresent();
        assertThat(limit.get().value()).isEqualTo(1000);
    }

    @Test
    void savesOverAnExistingRow() {
        PlatformLimitSpringDataRepository repository = mock(PlatformLimitSpringDataRepository.class);
        PlatformLimitJpaEntity entity = new PlatformLimitJpaEntity(LimitKey.MAX_RECEIPTS, 100, 0, NOW, null);
        when(repository.findById(LimitKey.MAX_RECEIPTS)).thenReturn(Optional.of(entity));
        PlatformLimitRepositoryAdapter adapter = new PlatformLimitRepositoryAdapter(repository);

        adapter.save(PlatformLimit.of(LimitKey.MAX_RECEIPTS, 100, NOW)
                .withValue(300, AggregateId.newId(), NOW.plusSeconds(1)));

        verify(repository).save(entity);
        assertThat(entity.getLimitValue()).isEqualTo(300);
    }

    @Test
    void savesANewRowWhenNoneExistedYet() {
        PlatformLimitSpringDataRepository repository = mock(PlatformLimitSpringDataRepository.class);
        when(repository.findById(LimitKey.MAX_UPLOADS)).thenReturn(Optional.empty());
        PlatformLimitRepositoryAdapter adapter = new PlatformLimitRepositoryAdapter(repository);

        adapter.save(PlatformLimit.of(LimitKey.MAX_UPLOADS, 50, NOW));

        verify(repository).save(any(PlatformLimitJpaEntity.class));
    }
}
