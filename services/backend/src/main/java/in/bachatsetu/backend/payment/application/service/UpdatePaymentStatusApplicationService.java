package in.bachatsetu.backend.payment.application.service;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.payment.application.command.UpdatePaymentStatusCommand;
import in.bachatsetu.backend.payment.application.mapper.PaymentApplicationMapper;
import in.bachatsetu.backend.payment.application.port.ClockPort;
import in.bachatsetu.backend.payment.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.payment.application.port.TransactionPort;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.usecase.UpdatePaymentStatusUseCase;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/** Coordinates payment status transitions without owning business invariants. */
public final class UpdatePaymentStatusApplicationService implements UpdatePaymentStatusUseCase {

    private final ClockPort clock;
    private final TransactionPort transaction;
    private final PaymentApplicationSupport support;
    private final CreateAuditEntryUseCase createAuditEntry;

    public UpdatePaymentStatusApplicationService(
            PaymentRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            PaymentApplicationMapper mapper,
            CreateAuditEntryUseCase createAuditEntry) {
        Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.support = new PaymentApplicationSupport(repository, eventPublisher, mapper);
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @Override
    public PaymentResult execute(UpdatePaymentStatusCommand command) {
        Objects.requireNonNull(command, "update command must not be null");
        PaymentResult result = transaction.execute(() -> update(command));
        if (command.targetStatus() == PaymentStatus.VERIFIED) {
            auditPaymentVerified(result);
        }
        return result;
    }

    private PaymentResult update(UpdatePaymentStatusCommand command) {
        Payment payment = support.requirePayment(command.tenantId(), command.paymentId());
        Instant now = clock.now();
        switch (command.targetStatus()) {
            case PENDING_PROVIDER -> payment.startAttempt(command.actorId(), now);
            case VERIFIED -> payment.verify(command.providerReference(), command.actorId(), now);
            case FAILED -> payment.fail(command.failureCode(), command.actorId(), now);
            case REFUNDED -> payment.refund(command.actorId(), now);
            default -> throw new IllegalArgumentException(
                    "unsupported target status: " + command.targetStatus());
        }
        return support.saveAndPublish(payment);
    }

    /**
     * Best-effort: an audit failure must never fail a payment verification that has already committed, so
     * any exception is caught and discarded here rather than propagated. The application layer must stay
     * framework-free (no logging framework), so a failure here is silent by design; a future sprint could
     * route this through a dedicated non-blocking diagnostics port if visibility is needed.
     */
    private void auditPaymentVerified(PaymentResult result) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    new AggregateId(result.tenantId()), new AggregateId(result.memberId()),
                    AuditEventType.PAYMENT_VERIFIED, "payment", "Payment", new AggregateId(result.paymentId()),
                    "PAYMENT_VERIFIED", "payment verified", null, null, null));
        } catch (RuntimeException exception) {
            // Audit is best-effort: never let a recording failure affect an already-committed payment.
        }
    }
}
