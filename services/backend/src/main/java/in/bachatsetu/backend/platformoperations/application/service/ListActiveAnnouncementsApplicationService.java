package in.bachatsetu.backend.platformoperations.application.service;

import in.bachatsetu.backend.platformoperations.application.mapper.PlatformOperationsApplicationMapper;
import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.application.query.AnnouncementResult;
import in.bachatsetu.backend.platformoperations.application.usecase.ListActiveAnnouncementsUseCase;
import in.bachatsetu.backend.platformoperations.domain.port.AnnouncementRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class ListActiveAnnouncementsApplicationService implements ListActiveAnnouncementsUseCase {

    private final AnnouncementRepository repository;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final PlatformOperationsApplicationMapper mapper;

    public ListActiveAnnouncementsApplicationService(
            AnnouncementRepository repository, ClockPort clock, TransactionPort transaction,
            PlatformOperationsApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public List<AnnouncementResult> execute() {
        return transaction.execute(() -> {
            Instant now = clock.now();
            return repository.findActive(now).stream().map(announcement -> mapper.toResult(announcement, now)).toList();
        });
    }
}
