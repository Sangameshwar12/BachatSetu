package in.bachatsetu.backend.platformoperations.application.service;

import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.application.query.PlatformOverviewResult;
import in.bachatsetu.backend.platformoperations.application.usecase.GetPlatformOverviewUseCase;
import in.bachatsetu.backend.platformoperations.domain.model.PlatformOverviewSnapshot;
import in.bachatsetu.backend.platformoperations.domain.port.PlatformOverviewRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class GetPlatformOverviewApplicationService implements GetPlatformOverviewUseCase {

    private final PlatformOverviewRepository repository;
    private final ClockPort clock;
    private final TransactionPort transaction;

    public GetPlatformOverviewApplicationService(
            PlatformOverviewRepository repository, ClockPort clock, TransactionPort transaction) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
    }

    @Override
    public PlatformOverviewResult execute() {
        return transaction.execute(() -> {
            Instant todayStart = clock.now().truncatedTo(ChronoUnit.DAYS);
            Instant todayEnd = todayStart.plus(1, ChronoUnit.DAYS);
            PlatformOverviewSnapshot snapshot = repository.compute(todayStart, todayEnd);
            return new PlatformOverviewResult(
                    snapshot.totalUsers(), snapshot.totalOrganizers(), snapshot.totalGroups(),
                    snapshot.totalMembers(), snapshot.totalPayments(), snapshot.totalReceipts(),
                    snapshot.totalNotifications(), snapshot.totalStoredFiles(), snapshot.totalActiveTenants(),
                    snapshot.totalRevenuePaise(), snapshot.todaySignups(), snapshot.todayPayments(),
                    snapshot.todayGroups(), snapshot.todayNotifications(), snapshot.todayStorageUploads());
        });
    }
}
