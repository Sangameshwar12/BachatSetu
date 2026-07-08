package in.bachatsetu.backend.paymentgateway.interfaces.rest.config;

import in.bachatsetu.backend.paymentgateway.application.port.ClockPort;
import in.bachatsetu.backend.paymentgateway.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentGatewayPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentRefundPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentWebhookVerifierPort;
import in.bachatsetu.backend.paymentgateway.application.port.TransactionPort;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter.ApplicationEventPaymentGatewayEventPublisherAdapter;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter.CashfreeWebhookVerifier;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter.RazorpayWebhookVerifier;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter.SimulatedCashfreeGatewayAdapter;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter.SimulatedCashfreeRefundAdapter;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter.SimulatedRazorpayGatewayAdapter;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter.SimulatedRazorpayRefundAdapter;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter.SimulatedStripeGatewayAdapter;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter.SimulatedStripeRefundAdapter;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter.SpringPaymentGatewayTransactionAdapter;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter.StripeWebhookVerifier;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter.SystemPaymentGatewayClockAdapter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Composes the Payment Gateway outbound port adapters: the standard Clock/Transaction/EventPublisher trio,
 * every provider's simulated order/refund adapter, and every provider's real HMAC-SHA256 webhook verifier.
 * Three beans exist for {@link PaymentGatewayPort} (and {@link PaymentRefundPort}/{@link
 * PaymentWebhookVerifierPort}) — one per provider — so that application code, which depends on {@code
 * List<PaymentGatewayPort>} etc., receives every provider and resolves the one it needs by
 * {@code GatewayType} at call time (see {@code GatewayPortResolver}).
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled}, matching every other module's
 * infrastructure config, for the same non-deterministic-condition-evaluation-order reason documented on
 * {@code PaymentInfrastructureConfig}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PaymentGatewayProperties.class)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PaymentGatewayInfrastructureConfig {

    @Bean
    Clock paymentGatewayClock() {
        return Clock.systemUTC();
    }

    @Bean
    TransactionTemplate paymentGatewayTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    ClockPort systemPaymentGatewayClockAdapter(Clock paymentGatewayClock) {
        return new SystemPaymentGatewayClockAdapter(paymentGatewayClock);
    }

    @Bean
    TransactionPort springPaymentGatewayTransactionAdapter(TransactionTemplate paymentGatewayTransactionTemplate) {
        return new SpringPaymentGatewayTransactionAdapter(paymentGatewayTransactionTemplate);
    }

    @Bean
    DomainEventPublisherPort applicationEventPaymentGatewayEventPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventPaymentGatewayEventPublisherAdapter(publisher);
    }

    @Bean
    PaymentGatewayPort simulatedRazorpayGatewayAdapter() {
        return new SimulatedRazorpayGatewayAdapter();
    }

    @Bean
    PaymentGatewayPort simulatedStripeGatewayAdapter() {
        return new SimulatedStripeGatewayAdapter();
    }

    @Bean
    PaymentGatewayPort simulatedCashfreeGatewayAdapter() {
        return new SimulatedCashfreeGatewayAdapter();
    }

    @Bean
    PaymentRefundPort simulatedRazorpayRefundAdapter() {
        return new SimulatedRazorpayRefundAdapter();
    }

    @Bean
    PaymentRefundPort simulatedStripeRefundAdapter() {
        return new SimulatedStripeRefundAdapter();
    }

    @Bean
    PaymentRefundPort simulatedCashfreeRefundAdapter() {
        return new SimulatedCashfreeRefundAdapter();
    }

    @Bean
    PaymentWebhookVerifierPort razorpayWebhookVerifier(PaymentGatewayProperties properties) {
        return new RazorpayWebhookVerifier(properties.razorpay().webhookSecret());
    }

    @Bean
    PaymentWebhookVerifierPort stripeWebhookVerifier(PaymentGatewayProperties properties) {
        return new StripeWebhookVerifier(properties.stripe().webhookSecret());
    }

    @Bean
    PaymentWebhookVerifierPort cashfreeWebhookVerifier(PaymentGatewayProperties properties) {
        return new CashfreeWebhookVerifier(properties.cashfree().webhookSecret());
    }
}
