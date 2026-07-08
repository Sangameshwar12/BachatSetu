package in.bachatsetu.backend.paymentgateway.domain.model;

import in.bachatsetu.backend.paymentgateway.domain.exception.InvalidGatewayOrderStateException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import java.time.Instant;
import java.util.Objects;

/**
 * Tracks one payment's gateway-side order: the provider's own order id and payment link, the provider's
 * most recently observed raw status string, and (once refunded) the provider's refund id.
 *
 * <p>Deliberately separate from the pre-existing {@code Payment} aggregate, which this module never
 * modifies directly — {@code GatewayOrder} references it only by {@link #paymentId()}, following this
 * codebase's existing rule that cross-module relationships are identifiers, not object references. Every
 * business-visible payment status transition (verified, failed, refunded) still happens on {@code Payment}
 * itself, through the pre-existing {@code UpdatePaymentStatusUseCase} — this aggregate only remembers the
 * gateway-integration metadata needed to talk to the provider again (to check status, or to refund).
 *
 * <p>{@code providerStatus} is stored as the provider's own raw string (for example Razorpay's
 * {@code "captured"}, Stripe's {@code "succeeded"}, Cashfree's {@code "SUCCESS"}) rather than mapped to a
 * shared enum: the three providers do not share a status vocabulary, and this field exists only to record
 * the most recent observation, not to drive further behavior.
 */
public final class GatewayOrder extends BaseAggregateRoot {

    private final AggregateId tenantId;
    private final AggregateId paymentId;
    private final GatewayType gatewayType;
    private final String providerOrderId;
    private final String paymentLink;
    private String providerStatus;
    private String providerRefundId;

    public GatewayOrder(
            AggregateId id,
            AggregateId tenantId,
            AggregateId paymentId,
            GatewayType gatewayType,
            String providerOrderId,
            String paymentLink,
            String providerStatus,
            String providerRefundId,
            AuditInfo auditInfo,
            long version) {
        super(id, auditInfo, version);
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId must not be null");
        this.gatewayType = Objects.requireNonNull(gatewayType, "gatewayType must not be null");
        this.providerOrderId = requireNonBlank(providerOrderId, "providerOrderId");
        this.paymentLink = paymentLink;
        this.providerStatus = providerStatus;
        this.providerRefundId = providerRefundId;
    }

    public static GatewayOrder create(
            AggregateId id,
            AggregateId tenantId,
            AggregateId paymentId,
            GatewayType gatewayType,
            String providerOrderId,
            String paymentLink,
            AggregateId actorId,
            Instant createdAt) {
        return new GatewayOrder(
                id, tenantId, paymentId, gatewayType, providerOrderId, paymentLink, null, null,
                AuditInfo.createdBy(actorId, createdAt), 0);
    }

    public void updateProviderStatus(String providerStatus, AggregateId actorId, Instant observedAt) {
        this.providerStatus = requireNonBlank(providerStatus, "providerStatus");
        markChanged(actorId, observedAt);
    }

    public void recordRefund(String providerRefundId, AggregateId actorId, Instant refundedAt) {
        if (this.providerRefundId != null) {
            throw new InvalidGatewayOrderStateException("gateway order has already recorded a refund");
        }
        this.providerRefundId = requireNonBlank(providerRefundId, "providerRefundId");
        markChanged(actorId, refundedAt);
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public AggregateId tenantId() { return tenantId; }
    public AggregateId paymentId() { return paymentId; }
    public GatewayType gatewayType() { return gatewayType; }
    public String providerOrderId() { return providerOrderId; }
    public String paymentLink() { return paymentLink; }
    public String providerStatus() { return providerStatus; }
    public String providerRefundId() { return providerRefundId; }
}
