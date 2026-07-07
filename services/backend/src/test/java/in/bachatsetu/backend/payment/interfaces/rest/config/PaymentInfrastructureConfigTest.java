package in.bachatsetu.backend.payment.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.payment.application.port.ClockPort;
import in.bachatsetu.backend.payment.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.payment.application.port.TransactionPort;
import in.bachatsetu.backend.payment.domain.factory.PaymentFactory;
import in.bachatsetu.backend.payment.interfaces.rest.adapter.ApplicationEventPaymentEventPublisherAdapter;
import in.bachatsetu.backend.payment.interfaces.rest.adapter.SpringPaymentTransactionAdapter;
import in.bachatsetu.backend.payment.interfaces.rest.adapter.SystemPaymentClockAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

class PaymentInfrastructureConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PaymentInfrastructureConfig.class);

    @Test
    void wiresEveryPortWhenTransactionManagerIsAvailable() {
        contextRunner
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(ClockPort.class).isInstanceOf(SystemPaymentClockAdapter.class);
                    assertThat(context).getBean(TransactionPort.class)
                            .isInstanceOf(SpringPaymentTransactionAdapter.class);
                    assertThat(context).getBean(DomainEventPublisherPort.class)
                            .isInstanceOf(ApplicationEventPaymentEventPublisherAdapter.class);
                    assertThat(context).getBean(PaymentFactory.class).isNotNull();
                });
    }

    @Test
    void doesNotWireAdaptersWithoutATransactionManager() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ClockPort.class);
            assertThat(context).doesNotHaveBean(TransactionPort.class);
            assertThat(context).doesNotHaveBean(DomainEventPublisherPort.class);
            assertThat(context).doesNotHaveBean(PaymentFactory.class);
        });
    }
}
