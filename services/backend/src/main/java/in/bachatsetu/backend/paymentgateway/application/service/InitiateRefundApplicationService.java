package in.bachatsetu.backend.paymentgateway.application.service;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.payment.application.command.UpdatePaymentStatusCommand;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.UpdatePaymentStatusUseCase;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.paymentgateway.application.command.InitiateRefundCommand;
import in.bachatsetu.backend.paymentgateway.application.exception.RefundNotAllowedException;
import in.bachatsetu.backend.paymentgateway.application.mapper.PaymentGatewayApplicationMapper;
import in.bachatsetu.backend.paymentgateway.application.port.ClockPort;
import in.bachatsetu.backend.paymentgateway.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentRefundPort;
import in.bachatsetu.backend.paymentgateway.application.port.TransactionPort;
import in.bachatsetu.backend.paymentgateway.application.query.RefundResult;
import in.bachatsetu.backend.paymentgateway.application.usecase.InitiateRefundUseCase;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayOrder;
import in.bachatsetu.backend.paymentgateway.domain.port.GatewayOrderRepository;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Initiates a full refund of a {@code VERIFIED} payment. The idempotency check runs before the
 * VERIFIED-status check: once a refund has been recorded on this payment's {@link GatewayOrder}, a repeated
 * call returns that same result immediately, even though the payment itself has by then moved on to
 * {@code REFUNDED} (which would otherwise fail the VERIFIED check) — the two checks answer different
 * questions ("did we already do this?" vs. "is this a legal new attempt?") and must run in that order for
 * idempotency to actually hold.
 */
public final class InitiateRefundApplicationService implements InitiateRefundUseCase {

    private final GatewayOrderRepository orderRepository;
    private final List<PaymentRefundPort> refundPorts;
    private final GetPaymentUseCase getPayment;
    private final UpdatePaymentStatusUseCase updatePaymentStatus;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final PaymentGatewayApplicationSupport support;
    private final CreateAuditEntryUseCase createAuditEntry;

    public InitiateRefundApplicationService(
            GatewayOrderRepository orderRepository,
            List<PaymentRefundPort> refundPorts,
            DomainEventPublisherPort eventPublisher,
            GetPaymentUseCase getPayment,
            UpdatePaymentStatusUseCase updatePaymentStatus,
            ClockPort clock,
            TransactionPort transaction,
            PaymentGatewayApplicationMapper mapper,
            CreateAuditEntryUseCase createAuditEntry) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "order repository must not be null");
        this.refundPorts = List.copyOf(Objects.requireNonNull(refundPorts, "refund ports must not be null"));
        this.getPayment = Objects.requireNonNull(getPayment, "get payment use case must not be null");
        this.updatePaymentStatus =
                Objects.requireNonNull(updatePaymentStatus, "update payment status use case must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.support = new PaymentGatewayApplicationSupport(orderRepository, eventPublisher, mapper);
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @Override
    public RefundResult execute(InitiateRefundCommand command) {
        Objects.requireNonNull(command, "refund command must not be null");
        RefundResult result = transaction.execute(() -> refund(command));
        auditGatewayRefundInitiated(command);
        return result;
    }

    private RefundResult refund(InitiateRefundCommand command) {
        GatewayOrder order = support.requireOrderByPayment(command.tenantId(), command.paymentId());
        if (order.providerRefundId() != null) {
            return new RefundResult(
                    command.paymentId().value(), order.gatewayType(), order.providerRefundId(), true);
        }

        PaymentResult payment = getPayment.execute(command.tenantId(), command.paymentId());
        if (PaymentStatus.valueOf(payment.status()) != PaymentStatus.VERIFIED) {
            throw new RefundNotAllowedException("only a VERIFIED payment can be refunded");
        }

        PaymentRefundPort refundPort = GatewayPortResolver.resolveRefundPort(refundPorts, order.gatewayType());
        Money amount = new Money(payment.amountPaise(), Currency.getInstance(payment.currencyCode()));
        RefundResult result = refundPort.initiateRefund(order.paymentId(), order.providerOrderId(), amount);

        Instant now = clock.now();
        order.recordRefund(result.providerRefundId(), command.actorId(), now);
        orderRepository.save(order);

        if (result.successful()) {
            updatePaymentStatus.execute(new UpdatePaymentStatusCommand(
                    command.tenantId(), command.paymentId(), PaymentStatus.REFUNDED, null, null,
                    command.actorId()));
        }
        return result;
    }

    /**
     * Best-effort: an audit failure must never fail a refund that has already committed, so any exception is
     * caught and discarded here rather than propagated.
     */
    private void auditGatewayRefundInitiated(InitiateRefundCommand command) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    command.tenantId(), command.actorId(), AuditEventType.GATEWAY_REFUND_INITIATED,
                    "paymentgateway", "Payment", command.paymentId(), "GATEWAY_REFUND_INITIATED",
                    "gateway refund initiated", null, null, null));
        } catch (RuntimeException exception) {
            // Audit is best-effort: never let a recording failure affect an already-initiated refund.
        }
    }
}
