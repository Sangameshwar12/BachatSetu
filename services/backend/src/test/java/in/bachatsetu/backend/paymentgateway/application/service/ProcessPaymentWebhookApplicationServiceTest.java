package in.bachatsetu.backend.paymentgateway.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.payment.application.command.UpdatePaymentStatusCommand;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.UpdatePaymentStatusUseCase;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.paymentgateway.application.command.ProcessWebhookCommand;
import in.bachatsetu.backend.paymentgateway.application.exception.GatewayOrderNotFoundException;
import in.bachatsetu.backend.paymentgateway.application.exception.InvalidWebhookSignatureException;
import in.bachatsetu.backend.paymentgateway.application.port.ClockPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentWebhookVerifierPort;
import in.bachatsetu.backend.paymentgateway.application.port.TransactionPort;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentStatusResult;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayOrder;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.paymentgateway.domain.port.GatewayOrderRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProcessPaymentWebhookApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private GatewayOrderRepository orderRepository;
    private PaymentWebhookVerifierPort verifier;
    private GetPaymentUseCase getPayment;
    private UpdatePaymentStatusUseCase updatePaymentStatus;
    private ClockPort clock;
    private TransactionPort transaction;
    private ProcessPaymentWebhookApplicationService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(GatewayOrderRepository.class);
        verifier = mock(PaymentWebhookVerifierPort.class);
        when(verifier.supportedProvider()).thenReturn(GatewayType.RAZORPAY);
        getPayment = mock(GetPaymentUseCase.class);
        updatePaymentStatus = mock(UpdatePaymentStatusUseCase.class);
        clock = () -> NOW;
        transaction = new DirectTransactionPort();
        service = new ProcessPaymentWebhookApplicationService(
                orderRepository, List.of(verifier), getPayment, updatePaymentStatus, clock, transaction);
    }

    @Test
    void verifiesAPaymentOnASuccessfulWebhook() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        GatewayOrder order = newOrder(tenantId, paymentId);
        when(verifier.verifySignature("body", "sig")).thenReturn(true);
        when(orderRepository.findByProviderOrderId(GatewayType.RAZORPAY, "order_1")).thenReturn(Optional.of(order));
        when(getPayment.execute(tenantId, paymentId)).thenReturn(newPaymentResult(paymentId, tenantId, "INITIATED"));

        PaymentStatusResult result = service.execute(new ProcessWebhookCommand(
                GatewayType.RAZORPAY, "body", "sig", "order_1", "SUCCESS", "ref-1"));

        assertThat(result.successful()).isTrue();
        ArgumentCaptor<UpdatePaymentStatusCommand> captor = ArgumentCaptor.forClass(UpdatePaymentStatusCommand.class);
        verify(updatePaymentStatus).execute(captor.capture());
        assertThat(captor.getValue().targetStatus()).isEqualTo(PaymentStatus.VERIFIED);
        verify(orderRepository).save(order);
    }

    @Test
    void isIdempotentWhenThePaymentIsAlreadyVerified() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        GatewayOrder order = newOrder(tenantId, paymentId);
        when(verifier.verifySignature("body", "sig")).thenReturn(true);
        when(orderRepository.findByProviderOrderId(GatewayType.RAZORPAY, "order_1")).thenReturn(Optional.of(order));
        when(getPayment.execute(tenantId, paymentId)).thenReturn(newPaymentResult(paymentId, tenantId, "VERIFIED"));

        PaymentStatusResult result = service.execute(new ProcessWebhookCommand(
                GatewayType.RAZORPAY, "body", "sig", "order_1", "SUCCESS", "ref-1"));

        assertThat(result.successful()).isTrue();
        verify(updatePaymentStatus, never()).execute(any());
    }

    @Test
    void failsAPaymentOnAFailureWebhook() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        GatewayOrder order = newOrder(tenantId, paymentId);
        when(verifier.verifySignature("body", "sig")).thenReturn(true);
        when(orderRepository.findByProviderOrderId(GatewayType.RAZORPAY, "order_1")).thenReturn(Optional.of(order));
        when(getPayment.execute(tenantId, paymentId)).thenReturn(newPaymentResult(paymentId, tenantId, "INITIATED"));

        PaymentStatusResult result = service.execute(new ProcessWebhookCommand(
                GatewayType.RAZORPAY, "body", "sig", "order_1", "FAILED", "ref-1"));

        assertThat(result.failed()).isTrue();
        ArgumentCaptor<UpdatePaymentStatusCommand> captor = ArgumentCaptor.forClass(UpdatePaymentStatusCommand.class);
        verify(updatePaymentStatus).execute(captor.capture());
        assertThat(captor.getValue().targetStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void rejectsAnInvalidSignatureBeforeTouchingAnyOrder() {
        when(verifier.verifySignature("body", "bad-sig")).thenReturn(false);

        assertThatThrownBy(() -> service.execute(new ProcessWebhookCommand(
                        GatewayType.RAZORPAY, "body", "bad-sig", "order_1", "SUCCESS", "ref-1")))
                .isInstanceOf(InvalidWebhookSignatureException.class);
        verify(orderRepository, never()).findByProviderOrderId(any(), any());
    }

    @Test
    void rejectsAnUnknownProviderOrder() {
        when(verifier.verifySignature("body", "sig")).thenReturn(true);
        when(orderRepository.findByProviderOrderId(GatewayType.RAZORPAY, "unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new ProcessWebhookCommand(
                        GatewayType.RAZORPAY, "body", "sig", "unknown", "SUCCESS", "ref-1")))
                .isInstanceOf(GatewayOrderNotFoundException.class);
    }

    private GatewayOrder newOrder(AggregateId tenantId, AggregateId paymentId) {
        return GatewayOrder.create(
                AggregateId.newId(), tenantId, paymentId, GatewayType.RAZORPAY, "order_1", "link",
                AggregateId.newId(), NOW);
    }

    private PaymentResult newPaymentResult(AggregateId paymentId, AggregateId tenantId, String status) {
        return new PaymentResult(
                paymentId.value(), tenantId.value(), UUID.randomUUID(), UUID.randomUUID(), "PAY-1",
                500_000L, "INR", "UPI", status, "NOT_REQUIRED", List.of(), NOW, NOW, 0);
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
