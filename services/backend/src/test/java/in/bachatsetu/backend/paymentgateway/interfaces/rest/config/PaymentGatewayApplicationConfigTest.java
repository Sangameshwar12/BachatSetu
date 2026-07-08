package in.bachatsetu.backend.paymentgateway.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.UpdatePaymentStatusUseCase;
import in.bachatsetu.backend.paymentgateway.application.mapper.PaymentGatewayApplicationMapper;
import in.bachatsetu.backend.paymentgateway.application.port.ClockPort;
import in.bachatsetu.backend.paymentgateway.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentGatewayPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentRefundPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentWebhookVerifierPort;
import in.bachatsetu.backend.paymentgateway.application.port.TransactionPort;
import in.bachatsetu.backend.paymentgateway.application.service.CreatePaymentOrderApplicationService;
import in.bachatsetu.backend.paymentgateway.application.service.InitiateRefundApplicationService;
import in.bachatsetu.backend.paymentgateway.application.service.ProcessPaymentWebhookApplicationService;
import in.bachatsetu.backend.paymentgateway.application.service.SyncPaymentStatusApplicationService;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.paymentgateway.domain.port.GatewayOrderRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentGatewayApplicationConfigTest {

    private final PaymentGatewayApplicationConfig config = new PaymentGatewayApplicationConfig();
    private final PaymentGatewayApplicationMapper mapper = config.paymentGatewayApplicationMapper();
    private final GetPaymentUseCase getPayment = mock(GetPaymentUseCase.class);
    private final UpdatePaymentStatusUseCase updatePaymentStatus = mock(UpdatePaymentStatusUseCase.class);
    private final GatewayOrderRepository orderRepository = mock(GatewayOrderRepository.class);
    private final DomainEventPublisherPort eventPublisher = mock(DomainEventPublisherPort.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = mock(TransactionPort.class);
    private final CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);
    private final PaymentGatewayProperties properties = new PaymentGatewayProperties(
            true, GatewayType.RAZORPAY,
            new PaymentGatewayProperties.Razorpay("", "", ""),
            new PaymentGatewayProperties.Stripe("", ""),
            new PaymentGatewayProperties.Cashfree("", "", ""));

    @Test
    void composesCreatePaymentOrderUseCase() {
        assertThat(config.createPaymentOrderUseCase(
                        getPayment, orderRepository, List.<PaymentGatewayPort>of(), eventPublisher, clock,
                        transaction, mapper, properties))
                .isInstanceOf(CreatePaymentOrderApplicationService.class);
    }

    @Test
    void composesProcessPaymentWebhookUseCase() {
        assertThat(config.processPaymentWebhookUseCase(
                        orderRepository, List.<PaymentWebhookVerifierPort>of(), getPayment, updatePaymentStatus,
                        clock, transaction, createAuditEntry))
                .isInstanceOf(ProcessPaymentWebhookApplicationService.class);
    }

    @Test
    void composesSyncPaymentStatusUseCase() {
        assertThat(config.syncPaymentStatusUseCase(
                        orderRepository, List.<PaymentGatewayPort>of(), eventPublisher, getPayment,
                        updatePaymentStatus, clock, transaction, mapper))
                .isInstanceOf(SyncPaymentStatusApplicationService.class);
    }

    @Test
    void composesInitiateRefundUseCase() {
        assertThat(config.initiateRefundUseCase(
                        orderRepository, List.<PaymentRefundPort>of(), eventPublisher, getPayment,
                        updatePaymentStatus, clock, transaction, mapper, createAuditEntry))
                .isInstanceOf(InitiateRefundApplicationService.class);
    }
}
