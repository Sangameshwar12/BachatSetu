package in.bachatsetu.backend.paymentgateway.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.UpdatePaymentStatusUseCase;
import in.bachatsetu.backend.paymentgateway.application.command.InitiateRefundCommand;
import in.bachatsetu.backend.paymentgateway.application.exception.RefundNotAllowedException;
import in.bachatsetu.backend.paymentgateway.application.mapper.PaymentGatewayApplicationMapper;
import in.bachatsetu.backend.paymentgateway.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentRefundPort;
import in.bachatsetu.backend.paymentgateway.application.port.TransactionPort;
import in.bachatsetu.backend.paymentgateway.application.query.RefundResult;
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

class InitiateRefundApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private GatewayOrderRepository orderRepository;
    private PaymentRefundPort refundPort;
    private GetPaymentUseCase getPayment;
    private UpdatePaymentStatusUseCase updatePaymentStatus;
    private CreateAuditEntryUseCase createAuditEntry;
    private InitiateRefundApplicationService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(GatewayOrderRepository.class);
        refundPort = mock(PaymentRefundPort.class);
        when(refundPort.supportedProvider()).thenReturn(GatewayType.RAZORPAY);
        getPayment = mock(GetPaymentUseCase.class);
        updatePaymentStatus = mock(UpdatePaymentStatusUseCase.class);
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        service = new InitiateRefundApplicationService(
                orderRepository, List.of(refundPort), mock(DomainEventPublisherPort.class), getPayment,
                updatePaymentStatus, () -> NOW, new DirectTransactionPort(), new PaymentGatewayApplicationMapper(),
                createAuditEntry);
    }

    @Test
    void refundsAVerifiedPayment() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        GatewayOrder order = newOrder(tenantId, paymentId);
        when(orderRepository.findByPaymentId(tenantId, paymentId)).thenReturn(Optional.of(order));
        when(getPayment.execute(tenantId, paymentId)).thenReturn(newPaymentResult(paymentId, tenantId, "VERIFIED"));
        when(refundPort.initiateRefund(any(), any(), any())).thenReturn(
                new RefundResult(paymentId.value(), GatewayType.RAZORPAY, "rfnd_1", true));

        RefundResult result = service.execute(new InitiateRefundCommand(tenantId, paymentId, AggregateId.newId()));

        assertThat(result.providerRefundId()).isEqualTo("rfnd_1");
        assertThat(result.successful()).isTrue();
        verify(updatePaymentStatus).execute(any());
        verify(orderRepository).save(order);
        verify(createAuditEntry).execute(any());
    }

    @Test
    void rejectsRefundingAPaymentThatIsNotVerified() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        GatewayOrder order = newOrder(tenantId, paymentId);
        when(orderRepository.findByPaymentId(tenantId, paymentId)).thenReturn(Optional.of(order));
        when(getPayment.execute(tenantId, paymentId)).thenReturn(newPaymentResult(paymentId, tenantId, "INITIATED"));

        assertThatThrownBy(() -> service.execute(new InitiateRefundCommand(tenantId, paymentId, AggregateId.newId())))
                .isInstanceOf(RefundNotAllowedException.class);
        verify(refundPort, never()).initiateRefund(any(), any(), any());
    }

    @Test
    void isIdempotentWhenTheOrderAlreadyRecordsARefund() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        GatewayOrder order = newOrder(tenantId, paymentId);
        order.recordRefund("rfnd_existing", AggregateId.newId(), NOW);
        when(orderRepository.findByPaymentId(tenantId, paymentId)).thenReturn(Optional.of(order));

        RefundResult result = service.execute(new InitiateRefundCommand(tenantId, paymentId, AggregateId.newId()));

        assertThat(result.providerRefundId()).isEqualTo("rfnd_existing");
        verify(refundPort, never()).initiateRefund(any(), any(), any());
        verify(getPayment, never()).execute(any(), any());
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
