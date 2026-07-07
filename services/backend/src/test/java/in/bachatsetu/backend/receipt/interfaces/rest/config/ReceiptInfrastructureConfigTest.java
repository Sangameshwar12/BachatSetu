package in.bachatsetu.backend.receipt.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.receipt.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.receipt.application.port.TransactionPort;
import in.bachatsetu.backend.receipt.domain.factory.ReceiptFactory;
import in.bachatsetu.backend.receipt.interfaces.rest.adapter.ApplicationEventReceiptEventPublisherAdapter;
import in.bachatsetu.backend.receipt.interfaces.rest.adapter.SpringReceiptTransactionAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

class ReceiptInfrastructureConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ReceiptInfrastructureConfig.class);

    @Test
    void wiresEveryPortWhenTransactionManagerIsAvailable() {
        contextRunner
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(TransactionPort.class)
                            .isInstanceOf(SpringReceiptTransactionAdapter.class);
                    assertThat(context).getBean(DomainEventPublisherPort.class)
                            .isInstanceOf(ApplicationEventReceiptEventPublisherAdapter.class);
                    assertThat(context).getBean(ReceiptFactory.class).isNotNull();
                });
    }

    @Test
    void doesNotWireAdaptersWithoutATransactionManager() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(TransactionPort.class);
            assertThat(context).doesNotHaveBean(DomainEventPublisherPort.class);
            assertThat(context).doesNotHaveBean(ReceiptFactory.class);
        });
    }
}
