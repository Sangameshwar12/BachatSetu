package in.bachatsetu.backend.paymentgateway.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.paymentgateway.application.port.ClockPort;
import in.bachatsetu.backend.paymentgateway.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentGatewayPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentRefundPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentWebhookVerifierPort;
import in.bachatsetu.backend.paymentgateway.application.port.TransactionPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

class PaymentGatewayInfrastructureConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PaymentGatewayInfrastructureConfig.class);

    @Test
    void wiresEveryPortAndProviderAdapterWhenTransactionManagerIsAvailable() {
        contextRunner
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .withPropertyValues(
                        "bachatsetu.payment.gateway.enabled=true",
                        "bachatsetu.payment.gateway.default-provider=RAZORPAY",
                        "bachatsetu.payment.gateway.razorpay.key-id=",
                        "bachatsetu.payment.gateway.razorpay.secret=",
                        "bachatsetu.payment.gateway.razorpay.webhook-secret=",
                        "bachatsetu.payment.gateway.stripe.api-key=",
                        "bachatsetu.payment.gateway.stripe.webhook-secret=",
                        "bachatsetu.payment.gateway.cashfree.client-id=",
                        "bachatsetu.payment.gateway.cashfree.client-secret=",
                        "bachatsetu.payment.gateway.cashfree.webhook-secret=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ClockPort.class);
                    assertThat(context).hasSingleBean(TransactionPort.class);
                    assertThat(context).hasSingleBean(DomainEventPublisherPort.class);
                    assertThat(context.getBeansOfType(PaymentGatewayPort.class)).hasSize(3);
                    assertThat(context.getBeansOfType(PaymentRefundPort.class)).hasSize(3);
                    assertThat(context.getBeansOfType(PaymentWebhookVerifierPort.class)).hasSize(3);
                });
    }

    @Test
    void doesNotWireAdaptersWhenPersistenceRepositoriesDisabled() {
        contextRunner
                .withPropertyValues("bachatsetu.persistence.repositories.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ClockPort.class);
                    assertThat(context.getBeansOfType(PaymentGatewayPort.class)).isEmpty();
                });
    }
}
