package in.bachatsetu.backend.admin.application.configuration.service;

import in.bachatsetu.backend.admin.application.configuration.command.UpdateConfigurationCommand;
import in.bachatsetu.backend.admin.application.configuration.mapper.PlatformConfigApplicationMapper;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformConfigurationResult;
import in.bachatsetu.backend.admin.application.configuration.usecase.UpdateConfigurationUseCase;
import in.bachatsetu.backend.admin.application.port.ClockPort;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.configuration.model.PlatformConfiguration;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformConfigurationRepository;
import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public final class UpdateConfigurationApplicationService implements UpdateConfigurationUseCase {

    private final PlatformConfigurationRepository repository;
    private final TransactionPort transaction;
    private final PlatformConfigApplicationMapper mapper;
    private final CreateAuditEntryUseCase createAuditEntry;
    private final ClockPort clock;

    public UpdateConfigurationApplicationService(
            PlatformConfigurationRepository repository,
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
    public PlatformConfigurationResult execute(UpdateConfigurationCommand command) {
        Objects.requireNonNull(command, "update configuration command must not be null");
        PlatformConfigurationResult result = transaction.execute(() -> {
            PlatformConfiguration configuration = repository.find();
            configuration.update(
                    command.defaultLanguage(),
                    command.otpExpirySeconds(),
                    command.defaultStorageProvider(),
                    command.defaultPaymentProvider(),
                    command.notificationRetryCount(),
                    command.maximumUploadSizeBytes(),
                    command.maximumMembersPerGroup(),
                    command.maximumGroupsPerOrganizer(),
                    command.maintenanceEnabled(),
                    command.maintenanceMessage(),
                    command.maintenanceStartAt(),
                    command.maintenanceEndAt(),
                    command.administratorId(),
                    clock.now());
            repository.save(configuration);
            return mapper.toResult(configuration);
        });
        auditConfigurationUpdated(command.administratorId());
        return result;
    }

    private void auditConfigurationUpdated(AggregateId administratorId) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, administratorId, AuditEventType.PLATFORM_CONFIGURATION_UPDATED, "admin",
                    "PlatformConfiguration", null, "PLATFORM_CONFIGURATION_UPDATED",
                    "updated platform configuration", null, null, null));
        } catch (RuntimeException exception) {
            // Audit is best-effort: never let a recording failure affect an already-applied configuration update.
        }
    }
}
