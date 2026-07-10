package in.bachatsetu.backend.platformoperations.application.service;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.platformoperations.application.command.PublishAnnouncementCommand;
import in.bachatsetu.backend.platformoperations.application.mapper.PlatformOperationsApplicationMapper;
import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
import in.bachatsetu.backend.platformoperations.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.application.query.AnnouncementResult;
import in.bachatsetu.backend.platformoperations.application.usecase.PublishAnnouncementUseCase;
import in.bachatsetu.backend.platformoperations.domain.model.Announcement;
import in.bachatsetu.backend.platformoperations.domain.port.AnnouncementRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/**
 * Records an {@code ANNOUNCEMENT_PUBLISHED} audit entry directly rather than through an Audit event
 * listener: see {@link SuspendTenantApplicationService}'s Javadoc for why.
 */
public final class PublishAnnouncementApplicationService implements PublishAnnouncementUseCase {

    private final AnnouncementRepository repository;
    private final DomainEventPublisherPort eventPublisher;
    private final CreateAuditEntryUseCase createAuditEntry;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final PlatformOperationsApplicationMapper mapper;

    public PublishAnnouncementApplicationService(
            AnnouncementRepository repository,
            DomainEventPublisherPort eventPublisher,
            CreateAuditEntryUseCase createAuditEntry,
            ClockPort clock,
            TransactionPort transaction,
            PlatformOperationsApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "createAuditEntry must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public AnnouncementResult execute(PublishAnnouncementCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        AnnouncementResult result = transaction.execute(() -> {
            Instant now = clock.now();
            Announcement announcement = Announcement.publish(
                    AggregateId.newId(), command.title(), command.message(), command.startAt(), command.endAt(),
                    command.severity(), command.actorId(), now);
            repository.save(announcement);
            eventPublisher.publish(announcement.pullDomainEvents());
            return mapper.toResult(announcement, now);
        });
        auditAnnouncementPublished(command, result);
        return result;
    }

    private void auditAnnouncementPublished(PublishAnnouncementCommand command, AnnouncementResult result) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, command.actorId(), AuditEventType.ANNOUNCEMENT_PUBLISHED, "platformoperations",
                    "Announcement", result.announcementId(), "ANNOUNCEMENT_PUBLISHED",
                    "published a platform announcement", null, null, null));
        } catch (RuntimeException exception) {
            // Audit is best-effort: never let a recording failure affect an already-published announcement.
        }
    }
}
