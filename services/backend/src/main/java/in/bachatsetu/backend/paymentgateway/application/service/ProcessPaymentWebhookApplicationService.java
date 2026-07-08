package in.bachatsetu.backend.paymentgateway.application.service;

import in.bachatsetu.backend.payment.application.command.UpdatePaymentStatusCommand;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.UpdatePaymentStatusUseCase;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.payment.domain.model.ProviderReference;
import in.bachatsetu.backend.paymentgateway.application.command.ProcessWebhookCommand;
import in.bachatsetu.backend.paymentgateway.application.exception.GatewayOrderNotFoundException;
import in.bachatsetu.backend.paymentgateway.application.exception.InvalidWebhookSignatureException;
import in.bachatsetu.backend.paymentgateway.application.port.ClockPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentWebhookVerifierPort;
import in.bachatsetu.backend.paymentgateway.application.port.TransactionPort;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentStatusResult;
import in.bachatsetu.backend.paymentgateway.application.usecase.ProcessPaymentWebhookUseCase;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayOrder;
import in.bachatsetu.backend.paymentgateway.domain.port.GatewayOrderRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Verifies and processes one inbound gateway webhook call: signature verification is mandatory and happens
 * before anything else is trusted; the payment it refers to is resolved only through the {@link
 * GatewayOrder} this module owns (webhooks identify a provider order, never one of our internal payment
 * ids); and the actual payment status transition is delegated entirely to the pre-existing {@link
 * UpdatePaymentStatusUseCase} — this service never touches the {@code Payment} aggregate or its repository.
 *
 * <p>Idempotent by construction: if the payment is already in the status this webhook reports, the
 * corresponding {@code Payment.verify}/{@code Payment.fail} call is skipped rather than attempted again
 * (which would otherwise throw, since both require the payment to still be in a pending state). A duplicate
 * webhook therefore succeeds without side effects the second time, exactly as required.
 */
public final class ProcessPaymentWebhookApplicationService implements ProcessPaymentWebhookUseCase {

    private final GatewayOrderRepository orderRepository;
    private final List<PaymentWebhookVerifierPort> verifiers;
    private final GetPaymentUseCase getPayment;
    private final UpdatePaymentStatusUseCase updatePaymentStatus;
    private final ClockPort clock;
    private final TransactionPort transaction;

    public ProcessPaymentWebhookApplicationService(
            GatewayOrderRepository orderRepository,
            List<PaymentWebhookVerifierPort> verifiers,
            GetPaymentUseCase getPayment,
            UpdatePaymentStatusUseCase updatePaymentStatus,
            ClockPort clock,
            TransactionPort transaction) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "order repository must not be null");
        this.verifiers = List.copyOf(Objects.requireNonNull(verifiers, "verifiers must not be null"));
        this.getPayment = Objects.requireNonNull(getPayment, "get payment use case must not be null");
        this.updatePaymentStatus =
                Objects.requireNonNull(updatePaymentStatus, "update payment status use case must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
    }

    @Override
    public PaymentStatusResult execute(ProcessWebhookCommand command) {
        Objects.requireNonNull(command, "webhook command must not be null");
        PaymentWebhookVerifierPort verifier = GatewayPortResolver.resolveVerifier(verifiers, command.provider());
        if (!verifier.verifySignature(command.rawPayload(), command.signatureHeader())) {
            throw new InvalidWebhookSignatureException(
                    "webhook signature verification failed for " + command.provider());
        }
        return transaction.execute(() -> process(command));
    }

    private PaymentStatusResult process(ProcessWebhookCommand command) {
        GatewayOrder order = orderRepository.findByProviderOrderId(command.provider(), command.providerOrderId())
                .orElseThrow(() -> new GatewayOrderNotFoundException(
                        "no payment is associated with provider order " + command.providerOrderId()));
        PaymentResult payment = getPayment.execute(order.tenantId(), order.paymentId());
        AggregateId actorId = new AggregateId(payment.memberId());
        Instant now = clock.now();

        order.updateProviderStatus(command.status(), actorId, now);
        orderRepository.save(order);

        PaymentStatus currentStatus = PaymentStatus.valueOf(payment.status());
        if (command.successful() && currentStatus != PaymentStatus.VERIFIED) {
            updatePaymentStatus.execute(new UpdatePaymentStatusCommand(
                    order.tenantId(), order.paymentId(), PaymentStatus.VERIFIED,
                    new ProviderReference(command.provider().name(), command.providerReferenceId()),
                    null, actorId));
        } else if (!command.successful() && currentStatus != PaymentStatus.FAILED) {
            updatePaymentStatus.execute(new UpdatePaymentStatusCommand(
                    order.tenantId(), order.paymentId(), PaymentStatus.FAILED, null,
                    command.providerReferenceId(), actorId));
        }

        return new PaymentStatusResult(
                order.paymentId().value(), command.provider(), command.providerOrderId(),
                command.status(), command.successful(), !command.successful());
    }
}
