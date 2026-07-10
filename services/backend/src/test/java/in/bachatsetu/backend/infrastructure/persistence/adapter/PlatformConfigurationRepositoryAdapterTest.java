package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.domain.configuration.model.PlatformConfiguration;
import in.bachatsetu.backend.infrastructure.persistence.entity.config.PlatformConfigurationJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PlatformConfigurationSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlatformConfigurationRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    @Test
    void findsAndRehydratesTheSingletonRow() {
        PlatformConfigurationSpringDataRepository repository = mock(PlatformConfigurationSpringDataRepository.class);
        PlatformConfigurationJpaEntity entity = new PlatformConfigurationJpaEntity();
        entity.update(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 10_485_760L, 100, 20, false, null, null, null, 0, NOW,
                null);
        when(repository.findById((short) 1)).thenReturn(Optional.of(entity));
        PlatformConfigurationRepositoryAdapter adapter = new PlatformConfigurationRepositoryAdapter(repository);

        PlatformConfiguration configuration = adapter.find();

        assertThat(configuration.defaultLanguage()).isEqualTo("ENGLISH");
        assertThat(configuration.otpExpirySeconds()).isEqualTo(300);
    }

    @Test
    void throwsWhenTheSingletonRowIsMissing() {
        PlatformConfigurationSpringDataRepository repository = mock(PlatformConfigurationSpringDataRepository.class);
        when(repository.findById((short) 1)).thenReturn(Optional.empty());
        PlatformConfigurationRepositoryAdapter adapter = new PlatformConfigurationRepositoryAdapter(repository);

        assertThatThrownBy(adapter::find).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void savesAnUpdatedConfigurationOverTheExistingRow() {
        PlatformConfigurationSpringDataRepository repository = mock(PlatformConfigurationSpringDataRepository.class);
        PlatformConfigurationJpaEntity entity = new PlatformConfigurationJpaEntity();
        entity.update(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 10_485_760L, 100, 20, false, null, null, null, 0, NOW,
                null);
        when(repository.findById((short) 1)).thenReturn(Optional.of(entity));
        PlatformConfigurationRepositoryAdapter adapter = new PlatformConfigurationRepositoryAdapter(repository);
        PlatformConfiguration configuration = PlatformConfiguration.of(
                "HINDI", 600, "AWS_S3", "STRIPE", 5, 20_000_000L, 200, 40, false, null, null, null, 1, NOW,
                AggregateId.newId());

        adapter.save(configuration);

        verify(repository).save(entity);
        assertThat(entity.getDefaultLanguage()).isEqualTo("HINDI");
    }
}
