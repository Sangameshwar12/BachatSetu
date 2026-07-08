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
import in.bachatsetu.backend.paymentgateway.application.command.CreatePaymentOrderCommand;
import in.bachatsetu.backend.paymentgateway.application.exception.AmountMismatchException;
import in.bachatsetu.backend.paymentgateway.application.mapper.PaymentGatewayApplicationMapper;
import in.bachatsetu.backend.paymentgateway.application.port.ClockPort;
import in.bachatsetu.backend.paymentgateway.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentGatewayPort;
import in.bachatsetu.backend.paymentgateway.application.port.TransactionPort;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentOrderResult;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayOrder;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.paymentgateway.domain.port.GatewayOrderRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreatePaymentOrderApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private GetPaymentUseCase getPayment;
    private GatewayOrderRepository orderRepository;
    private PaymentGatewayPort gateway;
    private DomainEventPublisherPort eventPublisher;
    private ClockPort clock;
    private TransactionPort transaction;
    private PaymentGatewayApplicationMapper mapper;
    private CreatePaymentOrderApplicationService service;

    @BeforeEach
    void setUp() {
        getPayment = mock(GetPaymentUseCase.class);
        orderRepository = mock(GatewayOrderRepository.class);
        gateway = mock(PaymentGatewayPort.class);
        when(gateway.supportedProvider()).thenReturn(GatewayType.RAZORPAY);
        eventPublisher = mock(DomainEventPublisherPort.class);
        clock = () -> NOW;
        transaction = new DirectTransactionPort();
        mapper = new PaymentGatewayApplicationMapper();
        service = new CreatePaymentOrderApplicationService(
                getPayment, orderRepository, List.of(gateway), eventPublisher, clock, transaction, mapper,
                GatewayType.RAZORPAY);
    }

    @Test
    void createsANewOrderWhenNoneExistsYet() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        PaymentResult payment = newPaymentResult(paymentId, tenantId, 500_000L, "INR");
        when(getPayment.execute(tenantId, paymentId)).thenReturn(payment);
        when(orderRepository.findByPaymentId(tenantId, paymentId)).thenReturn(Optional.empty());
        when(gateway.createOrder(paymentId, Money.inr(500_000L)))
                .thenReturn(new PaymentOrderResult(paymentId.value(), GatewayType.RAZORPAY, "order_1", "link"));

        PaymentOrderResult result = service.execute(new CreatePaymentOrderCommand(
                tenantId, paymentId, Money.inr(500_000L), AggregateId.newId()));

        assertThat(result.providerOrderId()).isEqualTo("order_1");
        ArgumentCaptor<GatewayOrder> captor = ArgumentCaptor.forClass(GatewayOrder.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().paymentId()).isEqualTo(paymentId);
    }

    @Test
    void isIdempotentWhenAnOrderAlreadyExists() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        GatewayOrder existing = GatewayOrder.create(
                AggregateId.newId(), tenantId, paymentId, GatewayType.RAZORPAY, "order_existing", "link",
                AggregateId.newId(), NOW);
        when(orderRepository.findByPaymentId(tenantId, paymentId)).thenReturn(Optional.of(existing));

        PaymentOrderResult result = service.execute(new CreatePaymentOrderCommand(
                tenantId, paymentId, Money.inr(500_000L), AggregateId.newId()));

        assertThat(result.providerOrderId()).isEqualTo("order_existing");
        verify(gateway, never()).createOrder(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void rejectsAConfirmedAmountThatDoesNotMatchThePayment() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        PaymentResult payment = newPaymentResult(paymentId, tenantId, 500_000L, "INR");
        when(getPayment.execute(tenantId, paymentId)).thenReturn(payment);
        when(orderRepository.findByPaymentId(tenantId, paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new CreatePaymentOrderCommand(
                        tenantId, paymentId, Money.inr(999_000L), AggregateId.newId())))
                .isInstanceOf(AmountMismatchException.class);
        verify(gateway, never()).createOrder(any(), any());
    }

    @Test
    void rejectsNullCommand() {
        assertThatThrownBy(() -> service.execute(null)).isInstanceOf(NullPointerException.class);
    }

    private PaymentResult newPaymentResult(AggregateId paymentId, AggregateId tenantId, long amountPaise, String currency) {
        return new PaymentResult(
                paymentId.value(), tenantId.value(), UUID.randomUUID(), UUID.randomUUID(), "PAY-1",
                amountPaise, currency, "UPI", "INITIATED", "NOT_REQUIRED", List.of(), NOW, NOW, 0);
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(java.util.function.Supplier<T> operation) {
            return operation.get();
        }
    }
}
