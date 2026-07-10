package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.domain.configuration.model.FeatureFlag;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import in.bachatsetu.backend.infrastructure.persistence.entity.config.FeatureFlagJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.FeatureFlagSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FeatureFlagRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    @Test
    void findsAllFlags() {
        FeatureFlagSpringDataRepository repository = mock(FeatureFlagSpringDataRepository.class);
        when(repository.findAll()).thenReturn(
                List.of(new FeatureFlagJpaEntity(FeatureKey.PAYMENTS, true, 0, NOW, null)));
        FeatureFlagRepositoryAdapter adapter = new FeatureFlagRepositoryAdapter(repository);

        List<FeatureFlag> flags = adapter.findAll();

        assertThat(flags).hasSize(1);
        assertThat(flags.get(0).key()).isEqualTo(FeatureKey.PAYMENTS);
    }

    @Test
    void findsByKey() {
        FeatureFlagSpringDataRepository repository = mock(FeatureFlagSpringDataRepository.class);
        when(repository.findById(FeatureKey.STORAGE)).thenReturn(
                Optional.of(new FeatureFlagJpaEntity(FeatureKey.STORAGE, false, 2, NOW, null)));
        FeatureFlagRepositoryAdapter adapter = new FeatureFlagRepositoryAdapter(repository);

        Optional<FeatureFlag> flag = adapter.findByKey(FeatureKey.STORAGE);

        assertThat(flag).isPresent();
        assertThat(flag.get().enabled()).isFalse();
    }

    @Test
    void savesOverAnExistingRow() {
        FeatureFlagSpringDataRepository repository = mock(FeatureFlagSpringDataRepository.class);
        FeatureFlagJpaEntity entity = new FeatureFlagJpaEntity(FeatureKey.AUDIT, true, 0, NOW, null);
        when(repository.findById(FeatureKey.AUDIT)).thenReturn(Optional.of(entity));
        FeatureFlagRepositoryAdapter adapter = new FeatureFlagRepositoryAdapter(repository);

        adapter.save(FeatureFlag.defaultEnabled(FeatureKey.AUDIT, NOW)
                .withEnabled(false, AggregateId.newId(), NOW.plusSeconds(1)));

        verify(repository).save(entity);
        assertThat(entity.isEnabled()).isFalse();
    }

    @Test
    void savesANewRowWhenNoneExistedYet() {
        FeatureFlagSpringDataRepository repository = mock(FeatureFlagSpringDataRepository.class);
        when(repository.findById(FeatureKey.SIGNUP)).thenReturn(Optional.empty());
        FeatureFlagRepositoryAdapter adapter = new FeatureFlagRepositoryAdapter(repository);

        adapter.save(FeatureFlag.defaultEnabled(FeatureKey.SIGNUP, NOW));

        verify(repository).save(any(FeatureFlagJpaEntity.class));
    }
}
