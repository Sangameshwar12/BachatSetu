package in.bachatsetu.backend.audit.application.service;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.mapper.AuditApplicationMapper;
import in.bachatsetu.backend.audit.application.port.AuditPublisherPort;
import in.bachatsetu.backend.audit.application.port.ClockPort;
import in.bachatsetu.backend.audit.application.port.TransactionPort;
import in.bachatsetu.backend.audit.application.query.AuditEntryResult;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEntry;
import in.bachatsetu.backend.audit.domain.port.AuditRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Records a new, immutable {@link AuditEntry}. */
public final class CreateAuditEntryApplicationService implements CreateAuditEntryUseCase {

    private final AuditRepository repository;
    private final AuditPublisherPort publisher;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final AuditApplicationMapper mapper;

    public CreateAuditEntryApplicationService(
            AuditRepository repository,
            AuditPublisherPort publisher,
            ClockPort clock,
            TransactionPort transaction,
            AuditApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public AuditEntryResult execute(CreateAuditEntryCommand command) {
        Objects.requireNonNull(command, "create command must not be null");
        return transaction.execute(() -> create(command));
    }

    private AuditEntryResult create(CreateAuditEntryCommand command) {
        AuditEntry entry = AuditEntry.record(
                AggregateId.newId(), command.tenantId(), command.actorId(), command.eventType(),
                command.moduleName(), command.resourceType(), command.resourceId(), command.action(),
                command.description(), command.ipAddress(), command.userAgent(), command.metadata(),
                clock.now());
        repository.save(entry);
        publisher.publish(entry);
        return mapper.toResult(entry);
    }
}
