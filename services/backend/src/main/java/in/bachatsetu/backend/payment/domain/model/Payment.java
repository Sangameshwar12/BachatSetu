package in.bachatsetu.backend.payment.domain.model;

import in.bachatsetu.backend.payment.domain.event.PaymentInitiated;
import in.bachatsetu.backend.payment.domain.event.PaymentStatusChanged;
import in.bachatsetu.backend.payment.domain.exception.InvalidPaymentStateException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Payment extends BaseAggregateRoot {

    private final AggregateId tenantId;
    private final AggregateId groupId;
    private final AggregateId memberId;
    private final PaymentReference reference;
    private final IdempotencyKey idempotencyKey;
    private final Money amount;
    private final PaymentMethod method;
    private PaymentStatus status;
    private ReconciliationStatus reconciliationStatus;
    private final List<PaymentAttempt> attempts;

    public Payment(
            AggregateId id,
            AggregateId tenantId,
            AggregateId groupId,
            AggregateId memberId,
            PaymentReference reference,
            IdempotencyKey idempotencyKey,
            Money amount,
            PaymentMethod method,
            PaymentStatus status,
            ReconciliationStatus reconciliationStatus,
            List<PaymentAttempt> attempts,
            AuditInfo auditInfo,
            long version) {
        super(id, auditInfo, version);
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
        this.memberId = Objects.requireNonNull(memberId, "memberId must not be null");
        this.reference = Objects.requireNonNull(reference, "reference must not be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        this.amount = requirePositive(amount);
        this.method = Objects.requireNonNull(method, "method must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.reconciliationStatus = Objects.requireNonNull(
                reconciliationStatus, "reconciliationStatus must not be null");
        this.attempts = new ArrayList<>(Objects.requireNonNull(attempts, "attempts must not be null"));
    }

    public static Payment initiate(
            AggregateId id,
            AggregateId tenantId,
            AggregateId groupId,
            AggregateId memberId,
            PaymentReference reference,
            IdempotencyKey idempotencyKey,
            Money amount,
            PaymentMethod method,
            AggregateId actorId,
            Instant initiatedAt) {
        Payment payment = new Payment(
                id,
                tenantId,
                groupId,
                memberId,
                reference,
                idempotencyKey,
                amount,
                method,
                PaymentStatus.INITIATED,
                ReconciliationStatus.NOT_REQUIRED,
                List.of(),
                AuditInfo.createdBy(actorId, initiatedAt),
                0);
        payment.registerEvent(new PaymentInitiated(
                UUID.randomUUID(), id, memberId, reference, amount, initiatedAt));
        return payment;
    }

    public PaymentAttempt startAttempt(AggregateId actorId, Instant attemptedAt) {
        requireStatus(PaymentStatus.INITIATED, PaymentStatus.PENDING_PROVIDER);
        PaymentAttempt attempt = new PaymentAttempt(
                AggregateId.newId(), attempts.size() + 1, attemptedAt, PaymentAttemptStatus.CREATED, null, null);
        attempts.add(attempt);
        changeStatus(PaymentStatus.PENDING_PROVIDER, actorId, attemptedAt);
        return attempt;
    }

    public void verify(ProviderReference providerReference, AggregateId actorId, Instant verifiedAt) {
        requireStatus(PaymentStatus.INITIATED, PaymentStatus.PENDING_PROVIDER);
        if (!attempts.isEmpty()) {
            attempts.getLast().markSucceeded(providerReference);
        }
        reconciliationStatus = ReconciliationStatus.MATCHED;
        changeStatus(PaymentStatus.VERIFIED, actorId, verifiedAt);
    }

    public void fail(String failureCode, AggregateId actorId, Instant failedAt) {
        requireStatus(PaymentStatus.INITIATED, PaymentStatus.PENDING_PROVIDER);
        if (!attempts.isEmpty()) {
            attempts.getLast().markFailed(failureCode);
        }
        changeStatus(PaymentStatus.FAILED, actorId, failedAt);
    }

    private void changeStatus(PaymentStatus nextStatus, AggregateId actorId, Instant changedAt) {
        if (status == nextStatus) {
            return;
        }
        PaymentStatus previousStatus = status;
        status = nextStatus;
        markChanged(actorId, changedAt);
        registerEvent(new PaymentStatusChanged(
                UUID.randomUUID(), id(), previousStatus, nextStatus, changedAt));
    }

    private void requireStatus(PaymentStatus... allowedStatuses) {
        for (PaymentStatus allowedStatus : allowedStatuses) {
            if (status == allowedStatus) {
                return;
            }
        }
        throw new InvalidPaymentStateException("payment state does not permit this operation");
    }

    private static Money requirePositive(Money value) {
        Objects.requireNonNull(value, "amount must not be null");
        if (!value.isPositive()) {
            throw new IllegalArgumentException("payment amount must be positive");
        }
        return value;
    }

    public AggregateId tenantId() { return tenantId; }
    public AggregateId groupId() { return groupId; }
    public AggregateId memberId() { return memberId; }
    public PaymentReference reference() { return reference; }
    public IdempotencyKey idempotencyKey() { return idempotencyKey; }
    public Money amount() { return amount; }
    public PaymentMethod method() { return method; }
    public PaymentStatus status() { return status; }
    public ReconciliationStatus reconciliationStatus() { return reconciliationStatus; }
    public List<PaymentAttempt> attempts() { return List.copyOf(attempts); }
}
