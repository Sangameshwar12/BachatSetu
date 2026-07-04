package in.bachatsetu.backend.receipt.domain.model;

import in.bachatsetu.backend.receipt.domain.event.ReceiptCancelled;
import in.bachatsetu.backend.receipt.domain.event.ReceiptGenerated;
import in.bachatsetu.backend.receipt.domain.exception.InvalidReceiptStateException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Receipt extends BaseAggregateRoot {

    private final AggregateId tenantId;
    private final AggregateId paymentId;
    private final AggregateId memberId;
    private final ReceiptNumber number;
    private final List<ReceiptLine> lines;
    private final Money total;
    private ReceiptStatus status;
    private String cancellationReason;

    public Receipt(
            AggregateId id,
            AggregateId tenantId,
            AggregateId paymentId,
            AggregateId memberId,
            ReceiptNumber number,
            List<ReceiptLine> lines,
            ReceiptStatus status,
            String cancellationReason,
            AuditInfo auditInfo,
            long version) {
        super(id, auditInfo, version);
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId must not be null");
        this.memberId = Objects.requireNonNull(memberId, "memberId must not be null");
        this.number = Objects.requireNonNull(number, "number must not be null");
        this.lines = List.copyOf(Objects.requireNonNull(lines, "lines must not be null"));
        if (this.lines.isEmpty()) {
            throw new IllegalArgumentException("receipt must contain at least one line");
        }
        this.total = calculateTotal(this.lines);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.cancellationReason = normalizeReason(cancellationReason);
    }

    public static Receipt generate(
            AggregateId id,
            AggregateId tenantId,
            AggregateId paymentId,
            AggregateId memberId,
            ReceiptNumber number,
            List<ReceiptLine> lines,
            AggregateId actorId,
            Instant generatedAt) {
        Receipt receipt = new Receipt(
                id,
                tenantId,
                paymentId,
                memberId,
                number,
                lines,
                ReceiptStatus.GENERATED,
                null,
                AuditInfo.createdBy(actorId, generatedAt),
                0);
        receipt.registerEvent(new ReceiptGenerated(
                UUID.randomUUID(), id, paymentId, number, receipt.total(), generatedAt));
        return receipt;
    }

    public void markDelivered(AggregateId actorId, Instant deliveredAt) {
        if (status != ReceiptStatus.GENERATED) {
            throw new InvalidReceiptStateException("only a generated receipt can be delivered");
        }
        status = ReceiptStatus.DELIVERED;
        markChanged(actorId, deliveredAt);
    }

    public void cancel(String reason, AggregateId actorId, Instant cancelledAt) {
        if (status == ReceiptStatus.CANCELLED) {
            throw new InvalidReceiptStateException("receipt is already cancelled");
        }
        cancellationReason = normalizeReason(Objects.requireNonNull(reason, "reason must not be null"));
        status = ReceiptStatus.CANCELLED;
        markChanged(actorId, cancelledAt);
        registerEvent(new ReceiptCancelled(
                UUID.randomUUID(), id(), number, cancellationReason, cancelledAt));
    }

    private static Money calculateTotal(List<ReceiptLine> lines) {
        Currency currency = lines.getFirst().amount().currency();
        Money total = Money.zero(currency);
        for (ReceiptLine line : lines) {
            total = total.add(line.amount());
        }
        return total;
    }

    private static String normalizeReason(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > 300) {
            throw new IllegalArgumentException("cancellation reason length is invalid");
        }
        return normalized;
    }

    public AggregateId tenantId() { return tenantId; }
    public AggregateId paymentId() { return paymentId; }
    public AggregateId memberId() { return memberId; }
    public ReceiptNumber number() { return number; }
    public List<ReceiptLine> lines() { return lines; }
    public Money total() { return total; }
    public ReceiptStatus status() { return status; }
    public String cancellationReason() { return cancellationReason; }
}
