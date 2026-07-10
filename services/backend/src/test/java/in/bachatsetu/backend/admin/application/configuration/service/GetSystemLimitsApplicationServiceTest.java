package in.bachatsetu.backend.admin.application.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.application.configuration.mapper.PlatformConfigApplicationMapper;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformLimitResult;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.configuration.model.LimitKey;
import in.bachatsetu.backend.admin.domain.configuration.model.PlatformLimit;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformLimitRepository;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class GetSystemLimitsApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    @Test
    void computesAndMapsAllSystemLimits() {
        PlatformLimitRepository repository = mock(PlatformLimitRepository.class);
        when(repository.findAll()).thenReturn(List.of(PlatformLimit.of(LimitKey.MAX_GROUPS, 500, NOW)));
        GetSystemLimitsApplicationService service = new GetSystemLimitsApplicationService(
                repository, new DirectTransactionPort(), new PlatformConfigApplicationMapper());

        List<PlatformLimitResult> results = service.execute();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).key()).isEqualTo("MAX_GROUPS");
        assertThat(results.get(0).value()).isEqualTo(500);
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
