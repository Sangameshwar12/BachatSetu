package in.bachatsetu.backend.paymentgateway.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.UpdatePaymentStatusUseCase;
import in.bachatsetu.backend.paymentgateway.application.command.SyncPaymentStatusCommand;
import in.bachatsetu.backend.paymentgateway.application.exception.GatewayOrderNotFoundException;
import in.bachatsetu.backend.paymentgateway.application.mapper.PaymentGatewayApplicationMapper;
import in.bachatsetu.backend.paymentgateway.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentGatewayPort;
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

class SyncPaymentStatusApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private GatewayOrderRepository orderRepository;
    private PaymentGatewayPort gateway;
    private GetPaymentUseCase getPayment;
    private UpdatePaymentStatusUseCase updatePaymentStatus;
    private SyncPaymentStatusApplicationService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(GatewayOrderRepository.class);
        gateway = mock(PaymentGatewayPort.class);
        when(gateway.supportedProvider()).thenReturn(GatewayType.RAZORPAY);
        getPayment = mock(GetPaymentUseCase.class);
        updatePaymentStatus = mock(UpdatePaymentStatusUseCase.class);
        service = new SyncPaymentStatusApplicationService(
                orderRepository, List.of(gateway), mock(DomainEventPublisherPort.class), getPayment,
                updatePaymentStatus, () -> NOW, new DirectTransactionPort(), new PaymentGatewayApplicationMapper());
    }

    @Test
    void verifiesAPaymentWhenTheGatewayReportsSuccess() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        GatewayOrder order = newOrder(tenantId, paymentId);
        when(orderRepository.findByPaymentId(tenantId, paymentId)).thenReturn(Optional.of(order));
        when(gateway.fetchStatus(paymentId, "order_1")).thenReturn(new PaymentStatusResult(
                paymentId.value(), GatewayType.RAZORPAY, "order_1", "captured", true, false));
        when(getPayment.execute(tenantId, paymentId)).thenReturn(newPaymentResult(paymentId, tenantId, "INITIATED"));

        PaymentStatusResult result = service.execute(new SyncPaymentStatusCommand(tenantId, paymentId, AggregateId.newId()));

        assertThat(result.successful()).isTrue();
        verify(updatePaymentStatus).execute(any());
        verify(orderRepository).save(order);
    }

    @Test
    void doesNothingWhenTheStatusIsUnchanged() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        GatewayOrder order = newOrder(tenantId, paymentId);
        when(orderRepository.findByPaymentId(tenantId, paymentId)).thenReturn(Optional.of(order));
        when(gateway.fetchStatus(paymentId, "order_1")).thenReturn(new PaymentStatusResult(
                paymentId.value(), GatewayType.RAZORPAY, "order_1", "captured", true, false));
        when(getPayment.execute(tenantId, paymentId)).thenReturn(newPaymentResult(paymentId, tenantId, "VERIFIED"));

        service.execute(new SyncPaymentStatusCommand(tenantId, paymentId, AggregateId.newId()));

        verify(updatePaymentStatus, never()).execute(any());
    }

    @Test
    void rejectsWhenNoGatewayOrderExists() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        when(orderRepository.findByPaymentId(tenantId, paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new SyncPaymentStatusCommand(tenantId, paymentId, AggregateId.newId())))
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
