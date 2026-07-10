package in.bachatsetu.backend.platformoperations.application.service;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.platformoperations.application.command.ActivateTenantCommand;
import in.bachatsetu.backend.platformoperations.application.exception.PlatformOperationsApplicationException;
import in.bachatsetu.backend.platformoperations.application.exception.PlatformOperationsFailureReason;
import in.bachatsetu.backend.platformoperations.application.mapper.PlatformOperationsApplicationMapper;
import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
import in.bachatsetu.backend.platformoperations.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.application.query.TenantResult;
import in.bachatsetu.backend.platformoperations.application.usecase.ActivateTenantUseCase;
import in.bachatsetu.backend.platformoperations.domain.model.Tenant;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatistics;
import in.bachatsetu.backend.platformoperations.domain.port.TenantRepository;
import in.bachatsetu.backend.platformoperations.domain.port.TenantStatisticsRepository;
import java.util.Objects;

/**
 * Records a {@code TENANT_ACTIVATED} audit entry directly rather than through an Audit event listener: see
 * {@link SuspendTenantApplicationService}'s Javadoc for why.
 */
public final class ActivateTenantApplicationService implements ActivateTenantUseCase {

    private final TenantRepository tenantRepository;
    private final TenantStatisticsRepository statisticsRepository;
    private final DomainEventPublisherPort eventPublisher;
    private final CreateAuditEntryUseCase createAuditEntry;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final PlatformOperationsApplicationMapper mapper;

    public ActivateTenantApplicationService(
            TenantRepository tenantRepository,
            TenantStatisticsRepository statisticsRepository,
            DomainEventPublisherPort eventPublisher,
            CreateAuditEntryUseCase createAuditEntry,
            ClockPort clock,
            TransactionPort transaction,
            PlatformOperationsApplicationMapper mapper) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository must not be null");
        this.statisticsRepository = Objects.requireNonNull(statisticsRepository, "statisticsRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "createAuditEntry must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public TenantResult execute(ActivateTenantCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        TenantResult result = transaction.execute(() -> {
            Tenant tenant = tenantRepository.findById(command.tenantId())
                    .orElseThrow(() -> new PlatformOperationsApplicationException(
                            PlatformOperationsFailureReason.TENANT_NOT_FOUND,
                            "no suspended tenant record exists for this identifier"));
            tenant.activate(command.actorId(), clock.now());
            tenantRepository.save(tenant);
            eventPublisher.publish(tenant.pullDomainEvents());
            TenantStatistics statistics = statisticsRepository.computeFor(command.tenantId());
            return mapper.toResult(tenant, statistics);
        });
        auditTenantActivated(command);
        return result;
    }

    private void auditTenantActivated(ActivateTenantCommand command) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    command.tenantId(), command.actorId(), AuditEventType.TENANT_ACTIVATED, "platformoperations",
                    "Tenant", command.tenantId(), "TENANT_ACTIVATED", "activated a tenant", null, null, null));
        } catch (RuntimeException exception) {
            // Audit is best-effort: never let a recording failure affect an already-applied activation.
        }
    }
}
