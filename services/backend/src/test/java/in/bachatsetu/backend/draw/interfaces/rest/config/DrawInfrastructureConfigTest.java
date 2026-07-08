package in.bachatsetu.backend.draw.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.draw.application.port.ClockPort;
import in.bachatsetu.backend.draw.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.draw.application.port.TransactionPort;
import in.bachatsetu.backend.draw.domain.factory.DrawFactory;
import in.bachatsetu.backend.draw.interfaces.rest.adapter.ApplicationEventDrawEventPublisherAdapter;
import in.bachatsetu.backend.draw.interfaces.rest.adapter.SpringDrawTransactionAdapter;
import in.bachatsetu.backend.draw.interfaces.rest.adapter.SystemDrawClockAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

class DrawInfrastructureConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DrawInfrastructureConfig.class);

    @Test
    void wiresEveryPortWhenTransactionManagerIsAvailable() {
        contextRunner
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(ClockPort.class).isInstanceOf(SystemDrawClockAdapter.class);
                    assertThat(context).getBean(TransactionPort.class)
                            .isInstanceOf(SpringDrawTransactionAdapter.class);
                    assertThat(context).getBean(DomainEventPublisherPort.class)
                            .isInstanceOf(ApplicationEventDrawEventPublisherAdapter.class);
                    assertThat(context).getBean(DrawFactory.class).isNotNull();
                });
    }

    @Test
    void doesNotWireAdaptersWhenPersistenceRepositoriesDisabled() {
        contextRunner
                .withPropertyValues("bachatsetu.persistence.repositories.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ClockPort.class);
                    assertThat(context).doesNotHaveBean(TransactionPort.class);
                    assertThat(context).doesNotHaveBean(DomainEventPublisherPort.class);
                    assertThat(context).doesNotHaveBean(DrawFactory.class);
                });
    }
}
