package in.bachatsetu.backend.platformoperations.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.application.query.PlatformOverviewResult;
import in.bachatsetu.backend.platformoperations.domain.model.PlatformOverviewSnapshot;
import in.bachatsetu.backend.platformoperations.domain.port.PlatformOverviewRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class GetPlatformOverviewApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T14:30:00Z");

    private final PlatformOverviewRepository repository = mock(PlatformOverviewRepository.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = new StubTransactionPort();
    private final GetPlatformOverviewApplicationService service =
            new GetPlatformOverviewApplicationService(repository, clock, transaction);

    @Test
    void computesTheOverviewForTodaysUtcWindow() {
        when(clock.now()).thenReturn(NOW);
        Instant todayStart = NOW.truncatedTo(ChronoUnit.DAYS);
        Instant todayEnd = todayStart.plus(1, ChronoUnit.DAYS);
        when(repository.compute(todayStart, todayEnd)).thenReturn(new PlatformOverviewSnapshot(
                10, 2, 3, 4, 5, 6, 7, 8, 9, 100_000, 1, 2, 3, 4, 5));

        PlatformOverviewResult result = service.execute();

        assertThat(result.totalUsers()).isEqualTo(10);
        assertThat(result.totalActiveTenants()).isEqualTo(9);
        assertThat(result.todaySignups()).isEqualTo(1);
    }

    @Test
    void alwaysComputesWithinASingleTransaction() {
        when(clock.now()).thenReturn(NOW);
        when(repository.compute(any(), any())).thenReturn(new PlatformOverviewSnapshot(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        service.execute();
    }

    private static final class StubTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
