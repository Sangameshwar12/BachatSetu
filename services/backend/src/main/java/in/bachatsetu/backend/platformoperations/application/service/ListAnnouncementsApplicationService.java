package in.bachatsetu.backend.platformoperations.application.service;

import in.bachatsetu.backend.platformoperations.application.mapper.PlatformOperationsApplicationMapper;
import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.application.query.AnnouncementResult;
import in.bachatsetu.backend.platformoperations.application.usecase.ListAnnouncementsUseCase;
import in.bachatsetu.backend.platformoperations.domain.model.Announcement;
import in.bachatsetu.backend.platformoperations.domain.port.AnnouncementRepository;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.shared.domain.PageQuery;
import java.time.Instant;
import java.util.Objects;

public final class ListAnnouncementsApplicationService implements ListAnnouncementsUseCase {

    private final AnnouncementRepository repository;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final PlatformOperationsApplicationMapper mapper;

    public ListAnnouncementsApplicationService(
            AnnouncementRepository repository, ClockPort clock, TransactionPort transaction,
            PlatformOperationsApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Page<AnnouncementResult> execute(PageQuery pageQuery) {
        Objects.requireNonNull(pageQuery, "pageQuery must not be null");
        return transaction.execute(() -> {
            Instant now = clock.now();
            Page<Announcement> page = repository.findAll(pageQuery);
            return new Page<>(
                    page.content().stream().map(announcement -> mapper.toResult(announcement, now)).toList(),
                    page.page(), page.size(), page.totalElements());
        });
    }
}
