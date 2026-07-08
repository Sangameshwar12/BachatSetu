package in.bachatsetu.backend.paymentgateway.application.service;

import in.bachatsetu.backend.payment.application.command.UpdatePaymentStatusCommand;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.UpdatePaymentStatusUseCase;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.payment.domain.model.ProviderReference;
import in.bachatsetu.backend.paymentgateway.application.command.SyncPaymentStatusCommand;
import in.bachatsetu.backend.paymentgateway.application.mapper.PaymentGatewayApplicationMapper;
import in.bachatsetu.backend.paymentgateway.application.port.ClockPort;
import in.bachatsetu.backend.paymentgateway.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentGatewayPort;
import in.bachatsetu.backend.paymentgateway.application.port.TransactionPort;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentStatusResult;
import in.bachatsetu.backend.paymentgateway.application.usecase.SyncPaymentStatusUseCase;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayOrder;
import in.bachatsetu.backend.paymentgateway.domain.port.GatewayOrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Pulls a payment's current status directly from its gateway — the explicit, manually triggered
 * counterpart to webhook-driven synchronization, for when a webhook call may have been missed. Shares the
 * exact same "only transition through {@link UpdatePaymentStatusUseCase}, only when the target status
 * differs from the current one" idempotency mechanics as {@link ProcessPaymentWebhookApplicationService}.
 */
public final class SyncPaymentStatusApplicationService implements SyncPaymentStatusUseCase {

    private final GatewayOrderRepository orderRepository;
    private final List<PaymentGatewayPort> gateways;
    private final GetPaymentUseCase getPayment;
    private final UpdatePaymentStatusUseCase updatePaymentStatus;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final PaymentGatewayApplicationSupport support;

    public SyncPaymentStatusApplicationService(
            GatewayOrderRepository orderRepository,
            List<PaymentGatewayPort> gateways,
            DomainEventPublisherPort eventPublisher,
            GetPaymentUseCase getPayment,
            UpdatePaymentStatusUseCase updatePaymentStatus,
            ClockPort clock,
            TransactionPort transaction,
            PaymentGatewayApplicationMapper mapper) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "order repository must not be null");
        this.gateways = List.copyOf(Objects.requireNonNull(gateways, "gateways must not be null"));
        this.getPayment = Objects.requireNonNull(getPayment, "get payment use case must not be null");
        this.updatePaymentStatus =
                Objects.requireNonNull(updatePaymentStatus, "update payment status use case must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.support = new PaymentGatewayApplicationSupport(orderRepository, eventPublisher, mapper);
    }

    @Override
    public PaymentStatusResult execute(SyncPaymentStatusCommand command) {
        Objects.requireNonNull(command, "sync command must not be null");
        return transaction.execute(() -> sync(command));
    }

    private PaymentStatusResult sync(SyncPaymentStatusCommand command) {
        GatewayOrder order = support.requireOrderByPayment(command.tenantId(), command.paymentId());
        PaymentGatewayPort gateway = GatewayPortResolver.resolveGateway(gateways, order.gatewayType());
        PaymentStatusResult fetched = gateway.fetchStatus(order.paymentId(), order.providerOrderId());

        Instant now = clock.now();
        order.updateProviderStatus(fetched.providerStatus(), command.actorId(), now);
        orderRepository.save(order);

        PaymentResult payment = getPayment.execute(command.tenantId(), command.paymentId());
        PaymentStatus currentStatus = PaymentStatus.valueOf(payment.status());
        if (fetched.successful() && currentStatus != PaymentStatus.VERIFIED) {
            updatePaymentStatus.execute(new UpdatePaymentStatusCommand(
                    command.tenantId(), command.paymentId(), PaymentStatus.VERIFIED,
                    new ProviderReference(order.gatewayType().name(), order.providerOrderId()),
                    null, command.actorId()));
        } else if (fetched.failed() && currentStatus != PaymentStatus.FAILED) {
            updatePaymentStatus.execute(new UpdatePaymentStatusCommand(
                    command.tenantId(), command.paymentId(), PaymentStatus.FAILED, null,
                    "sync-reported-failure", command.actorId()));
        }
        return fetched;
    }
}
