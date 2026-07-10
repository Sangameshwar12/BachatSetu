package in.bachatsetu.backend.admin.application.configuration.service;

import in.bachatsetu.backend.admin.application.configuration.command.UpdateFeatureFlagsCommand;
import in.bachatsetu.backend.admin.application.configuration.mapper.PlatformConfigApplicationMapper;
import in.bachatsetu.backend.admin.application.configuration.query.FeatureFlagResult;
import in.bachatsetu.backend.admin.application.configuration.usecase.UpdateFeatureFlagsUseCase;
import in.bachatsetu.backend.admin.application.port.ClockPort;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureFlag;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import in.bachatsetu.backend.admin.domain.configuration.port.FeatureFlagRepository;
import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class UpdateFeatureFlagsApplicationService implements UpdateFeatureFlagsUseCase {

    private final FeatureFlagRepository repository;
    private final TransactionPort transaction;
    private final PlatformConfigApplicationMapper mapper;
    private final CreateAuditEntryUseCase createAuditEntry;
    private final ClockPort clock;

    public UpdateFeatureFlagsApplicationService(
            FeatureFlagRepository repository,
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
    public List<FeatureFlagResult> execute(UpdateFeatureFlagsCommand command) {
        Objects.requireNonNull(command, "update feature flags command must not be null");
        List<FeatureFlagResult> result = transaction.execute(() -> {
            for (Map.Entry<FeatureKey, Boolean> change : command.changes().entrySet()) {
                FeatureFlag current = repository.findByKey(change.getKey())
                        .orElseGet(() -> FeatureFlag.defaultEnabled(change.getKey(), clock.now()));
                FeatureFlag updated =
                        current.withEnabled(change.getValue(), command.administratorId(), clock.now());
                repository.save(updated);
            }
            return repository.findAll().stream().map(mapper::toResult).toList();
        });
        auditFeatureFlagsUpdated(command);
        return result;
    }

    private void auditFeatureFlagsUpdated(UpdateFeatureFlagsCommand command) {
        for (Map.Entry<FeatureKey, Boolean> change : command.changes().entrySet()) {
            auditOneFlagChange(command.administratorId(), change.getKey(), change.getValue());
        }
    }

    private void auditOneFlagChange(AggregateId administratorId, FeatureKey key, boolean enabled) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, administratorId, AuditEventType.FEATURE_FLAG_UPDATED, "admin", "FeatureFlag", null,
                    "FEATURE_FLAG_UPDATED", "set feature " + key.name() + " enabled=" + enabled, null, null,
                    "{\"key\":\"" + key.name() + "\",\"enabled\":" + enabled + "}"));
        } catch (RuntimeException exception) {
            // Audit is best-effort: never let a recording failure affect an already-applied flag change.
        }
    }
}
