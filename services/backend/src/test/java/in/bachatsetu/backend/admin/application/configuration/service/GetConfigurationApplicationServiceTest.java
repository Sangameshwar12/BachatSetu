package in.bachatsetu.backend.admin.application.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.application.configuration.mapper.PlatformConfigApplicationMapper;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformConfigurationResult;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.configuration.model.PlatformConfiguration;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformConfigurationRepository;
import java.time.Instant;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class GetConfigurationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    @Test
    void computesAndMapsTheConfiguration() {
        PlatformConfigurationRepository repository = mock(PlatformConfigurationRepository.class);
        when(repository.find()).thenReturn(PlatformConfiguration.of(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 10_485_760L, 100, 20, false, null, null, null, 0, NOW,
                null));
        GetConfigurationApplicationService service = new GetConfigurationApplicationService(
                repository, new DirectTransactionPort(), new PlatformConfigApplicationMapper());

        PlatformConfigurationResult result = service.execute();

        assertThat(result.defaultLanguage()).isEqualTo("ENGLISH");
        assertThat(result.otpExpirySeconds()).isEqualTo(300);
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
