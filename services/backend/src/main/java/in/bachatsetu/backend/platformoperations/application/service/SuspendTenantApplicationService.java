package in.bachatsetu.backend.platformoperations.application.service;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.platformoperations.application.command.SuspendTenantCommand;
import in.bachatsetu.backend.platformoperations.application.mapper.PlatformOperationsApplicationMapper;
import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
import in.bachatsetu.backend.platformoperations.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.application.query.TenantResult;
import in.bachatsetu.backend.platformoperations.application.usecase.SuspendTenantUseCase;
import in.bachatsetu.backend.platformoperations.domain.model.Tenant;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatistics;
import in.bachatsetu.backend.platformoperations.domain.port.TenantRepository;
import in.bachatsetu.backend.platformoperations.domain.port.TenantStatisticsRepository;
import java.util.Objects;

/**
 * Suspends a tenant, lazily creating its lifecycle record on first use: this codebase has no dedicated
 * Tenant table until a platform operations action is taken against one (see {@link Tenant}'s class Javadoc).
 *
 * <p>Records a {@code TENANT_SUSPENDED} audit entry directly rather than through an Audit event listener:
 * this module already depends on Notification (for broadcasts), which itself depends on Audit, so a
 * dependency from Audit back onto this module would form a cycle.
 */
public final class SuspendTenantApplicationService implements SuspendTenantUseCase {

    private final TenantRepository tenantRepository;
    private final TenantStatisticsRepository statisticsRepository;
    private final DomainEventPublisherPort eventPublisher;
    private final CreateAuditEntryUseCase createAuditEntry;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final PlatformOperationsApplicationMapper mapper;

    public SuspendTenantApplicationService(
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
    public TenantResult execute(SuspendTenantCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        TenantResult result = transaction.execute(() -> {
            Tenant tenant = tenantRepository.findById(command.tenantId())
                    .orElseGet(() -> Tenant.createActive(command.tenantId(), command.actorId(), clock.now()));
            tenant.suspend(command.reason(), command.actorId(), clock.now());
            tenantRepository.save(tenant);
            eventPublisher.publish(tenant.pullDomainEvents());
            TenantStatistics statistics = statisticsRepository.computeFor(command.tenantId());
            return mapper.toResult(tenant, statistics);
        });
        auditTenantSuspended(command);
        return result;
    }

    private void auditTenantSuspended(SuspendTenantCommand command) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    command.tenantId(), command.actorId(), AuditEventType.TENANT_SUSPENDED, "platformoperations",
                    "Tenant", command.tenantId(), "TENANT_SUSPENDED", "suspended a tenant: " + command.reason(),
                    null, null, null));
        } catch (RuntimeException exception) {
            // Audit is best-effort: never let a recording failure affect an already-applied suspension.
        }
    }
}
