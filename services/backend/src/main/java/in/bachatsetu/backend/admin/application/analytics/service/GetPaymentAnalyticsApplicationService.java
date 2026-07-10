package in.bachatsetu.backend.admin.application.analytics.service;

import in.bachatsetu.backend.admin.application.analytics.command.ViewAnalyticsCommand;
import in.bachatsetu.backend.admin.application.analytics.mapper.AnalyticsApplicationMapper;
import in.bachatsetu.backend.admin.application.analytics.query.PaymentAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetPaymentAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.analytics.port.PaymentAnalyticsRepository;
import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Computes payment analytics, then best-effort records {@code ADMIN_ANALYTICS_VIEWED}. */
public final class GetPaymentAnalyticsApplicationService implements GetPaymentAnalyticsUseCase {

    private final PaymentAnalyticsRepository repository;
    private final TransactionPort transaction;
    private final AnalyticsApplicationMapper mapper;
    private final CreateAuditEntryUseCase createAuditEntry;

    public GetPaymentAnalyticsApplicationService(
            PaymentAnalyticsRepository repository, TransactionPort transaction, AnalyticsApplicationMapper mapper,
            CreateAuditEntryUseCase createAuditEntry) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @Override
    public PaymentAnalyticsResult execute(ViewAnalyticsCommand command) {
        Objects.requireNonNull(command, "view analytics command must not be null");
        PaymentAnalyticsResult result = transaction.execute(() -> mapper.toResult(repository.compute()));
        auditAnalyticsViewed(command.administratorId());
        return result;
    }

    private void auditAnalyticsViewed(AggregateId administratorId) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, administratorId, AuditEventType.ADMIN_ANALYTICS_VIEWED, "admin", "Analytics", null,
                    "ADMIN_ANALYTICS_VIEWED", "viewed payment analytics", null, null, "{\"view\":\"payments\"}"));
        } catch (RuntimeException exception) {
            // Audit is best-effort: never let a recording failure affect an already-computed analytics view.
        }
    }
}
