package in.bachatsetu.backend.admin.application.configuration.service;

import in.bachatsetu.backend.admin.application.configuration.command.UpdateSystemLimitsCommand;
import in.bachatsetu.backend.admin.application.configuration.mapper.PlatformConfigApplicationMapper;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformLimitResult;
import in.bachatsetu.backend.admin.application.configuration.usecase.UpdateSystemLimitsUseCase;
import in.bachatsetu.backend.admin.application.port.ClockPort;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.configuration.model.LimitKey;
import in.bachatsetu.backend.admin.domain.configuration.model.PlatformLimit;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformLimitRepository;
import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class UpdateSystemLimitsApplicationService implements UpdateSystemLimitsUseCase {

    private final PlatformLimitRepository repository;
    private final TransactionPort transaction;
    private final PlatformConfigApplicationMapper mapper;
    private final CreateAuditEntryUseCase createAuditEntry;
    private final ClockPort clock;

    public UpdateSystemLimitsApplicationService(
            PlatformLimitRepository repository,
            TransactionPort transaction,
            PlatformConfigApplicationMapper mapper,
            CreateAuditEntryUseCase createAuditEntry,
            ClockPort clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "createAuditEntry must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public List<PlatformLimitResult> execute(UpdateSystemLimitsCommand command) {
        Objects.requireNonNull(command, "update system limits command must not be null");
        List<PlatformLimitResult> result = transaction.execute(() -> {
            for (Map.Entry<LimitKey, Long> change : command.changes().entrySet()) {
                PlatformLimit current = repository.findByKey(change.getKey())
                        .orElseGet(() -> PlatformLimit.of(change.getKey(), change.getValue(), clock.now()));
                PlatformLimit updated =
                        current.withValue(change.getValue(), command.administratorId(), clock.now());
                repository.save(updated);
            }
            return repository.findAll().stream().map(mapper::toResult).toList();
        });
        auditLimitsUpdated(command);
        return result;
    }

    private void auditLimitsUpdated(UpdateSystemLimitsCommand command) {
        for (Map.Entry<LimitKey, Long> change : command.changes().entrySet()) {
            auditOneLimitChange(command.administratorId(), change.getKey(), change.getValue());
        }
    }

    private void auditOneLimitChange(AggregateId administratorId, LimitKey key, long value) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, administratorId, AuditEventType.SYSTEM_LIMIT_UPDATED, "admin", "PlatformLimit", null,
                    "SYSTEM_LIMIT_UPDATED", "set limit " + key.name() + " to " + value, null, null,
                    "{\"key\":\"" + key.name() + "\",\"value\":" + value + "}"));
        } catch (RuntimeException exception) {
            // Audit is best-effort: never let a recording failure affect an already-applied limit change.
        }
    }
}
