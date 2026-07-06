package in.bachatsetu.backend.infrastructure.group.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.group.application.port.ClockPort;
import in.bachatsetu.backend.group.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.group.application.port.GroupCodeGeneratorPort;
import in.bachatsetu.backend.group.application.port.TransactionPort;
import in.bachatsetu.backend.infrastructure.group.adapter.ApplicationEventDomainEventPublisherAdapter;
import in.bachatsetu.backend.infrastructure.group.adapter.SavingsGroupCodeGeneratorAdapter;
import in.bachatsetu.backend.infrastructure.group.adapter.SpringGroupTransactionAdapter;
import in.bachatsetu.backend.infrastructure.group.adapter.SystemGroupClockAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

class GroupInfrastructureConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(GroupInfrastructureConfig.class);

    @Test
    void wiresEveryPortWhenTransactionManagerIsAvailable() {
        contextRunner
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(ClockPort.class).isInstanceOf(SystemGroupClockAdapter.class);
                    assertThat(context).getBean(TransactionPort.class).isInstanceOf(SpringGroupTransactionAdapter.class);
                    assertThat(context).getBean(GroupCodeGeneratorPort.class)
                            .isInstanceOf(SavingsGroupCodeGeneratorAdapter.class);
                    assertThat(context).getBean(DomainEventPublisherPort.class)
                            .isInstanceOf(ApplicationEventDomainEventPublisherAdapter.class);
                });
    }

    @Test
    void doesNotWireAdaptersWithoutATransactionManager() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ClockPort.class);
            assertThat(context).doesNotHaveBean(TransactionPort.class);
            assertThat(context).doesNotHaveBean(GroupCodeGeneratorPort.class);
            assertThat(context).doesNotHaveBean(DomainEventPublisherPort.class);
        });
    }
}
