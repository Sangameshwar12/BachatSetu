package in.bachatsetu.backend.payment.application.mapper;

import in.bachatsetu.backend.payment.application.query.PaymentAttemptResult;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.query.PaymentSummary;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentAttempt;
import java.util.Objects;

/** Maps the Payment domain aggregate to immutable application query models. */
public final class PaymentApplicationMapper {

    public PaymentResult toResult(Payment payment) {
        Objects.requireNonNull(payment, "payment must not be null");
        return new PaymentResult(
                payment.id().value(),
                payment.tenantId().value(),
                payment.groupId().value(),
                payment.memberId().value(),
                payment.reference().value(),
                payment.amount().minorUnits(),
                payment.amount().currency().getCurrencyCode(),
                payment.method().name(),
                payment.status().name(),
                payment.reconciliationStatus().name(),
                payment.attempts().stream().map(this::toAttemptResult).toList(),
                payment.auditInfo().createdAt(),
                payment.auditInfo().updatedAt(),
                payment.version());
    }

    public PaymentSummary toSummary(Payment payment) {
        Objects.requireNonNull(payment, "payment must not be null");
        return new PaymentSummary(
                payment.id().value(),
                payment.reference().value(),
                payment.amount().minorUnits(),
                payment.amount().currency().getCurrencyCode(),
                payment.method().name(),
                payment.status().name(),
                payment.auditInfo().createdAt());
    }

    public PaymentAttemptResult toAttemptResult(PaymentAttempt attempt) {
        Objects.requireNonNull(attempt, "attempt must not be null");
        return new PaymentAttemptResult(
                attempt.id().value(),
                attempt.sequence(),
                attempt.initiatedAt(),
                attempt.status().name(),
                attempt.providerReference() == null ? null : attempt.providerReference().provider(),
                attempt.providerReference() == null ? null : attempt.providerReference().transactionId(),
                attempt.failureCode());
    }
}
