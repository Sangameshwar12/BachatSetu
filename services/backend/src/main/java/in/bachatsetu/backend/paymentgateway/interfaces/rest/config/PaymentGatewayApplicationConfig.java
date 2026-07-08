package in.bachatsetu.backend.paymentgateway.interfaces.rest.config;

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
import in.bachatsetu.backend.paymentgateway.application.usecase.CreatePaymentOrderUseCase;
import in.bachatsetu.backend.paymentgateway.application.usecase.InitiateRefundUseCase;
import in.bachatsetu.backend.paymentgateway.application.usecase.ProcessPaymentWebhookUseCase;
import in.bachatsetu.backend.paymentgateway.application.usecase.SyncPaymentStatusUseCase;
import in.bachatsetu.backend.paymentgateway.domain.port.GatewayOrderRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes framework-free Payment Gateway application services. Reuses the pre-existing {@link
 * GetPaymentUseCase}/{@link UpdatePaymentStatusUseCase} beans registered by {@code PaymentApplicationConfig}
 * rather than declaring duplicates — every payment status transition still flows through the Payment
 * module's own use case boundary, never bypassed.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled}, matching every other module's
 * application config.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PaymentGatewayApplicationConfig {

    @Bean
    public PaymentGatewayApplicationMapper paymentGatewayApplicationMapper() {
        return new PaymentGatewayApplicationMapper();
    }

    @Bean
    public CreatePaymentOrderUseCase createPaymentOrderUseCase(
            GetPaymentUseCase getPayment,
            GatewayOrderRepository orderRepository,
            List<PaymentGatewayPort> gateways,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            PaymentGatewayApplicationMapper mapper,
            PaymentGatewayProperties properties) {
        return new CreatePaymentOrderApplicationService(
                getPayment, orderRepository, gateways, eventPublisher, clock, transaction, mapper,
                properties.defaultProvider());
    }

    @Bean
    public ProcessPaymentWebhookUseCase processPaymentWebhookUseCase(
            GatewayOrderRepository orderRepository,
            List<PaymentWebhookVerifierPort> verifiers,
            GetPaymentUseCase getPayment,
            UpdatePaymentStatusUseCase updatePaymentStatus,
            ClockPort clock,
            TransactionPort transaction,
            CreateAuditEntryUseCase createAuditEntry) {
        return new ProcessPaymentWebhookApplicationService(
                orderRepository, verifiers, getPayment, updatePaymentStatus, clock, transaction, createAuditEntry);
    }

    @Bean
    public SyncPaymentStatusUseCase syncPaymentStatusUseCase(
            GatewayOrderRepository orderRepository,
            List<PaymentGatewayPort> gateways,
            DomainEventPublisherPort eventPublisher,
            GetPaymentUseCase getPayment,
            UpdatePaymentStatusUseCase updatePaymentStatus,
            ClockPort clock,
            TransactionPort transaction,
            PaymentGatewayApplicationMapper mapper) {
        return new SyncPaymentStatusApplicationService(
                orderRepository, gateways, eventPublisher, getPayment, updatePaymentStatus, clock, transaction,
                mapper);
    }

    @Bean
    public InitiateRefundUseCase initiateRefundUseCase(
            GatewayOrderRepository orderRepository,
            List<PaymentRefundPort> refundPorts,
            DomainEventPublisherPort eventPublisher,
            GetPaymentUseCase getPayment,
            UpdatePaymentStatusUseCase updatePaymentStatus,
            ClockPort clock,
            TransactionPort transaction,
            PaymentGatewayApplicationMapper mapper,
            CreateAuditEntryUseCase createAuditEntry) {
        return new InitiateRefundApplicationService(
                orderRepository, refundPorts, eventPublisher, getPayment, updatePaymentStatus, clock, transaction,
                mapper, createAuditEntry);
    }
}
