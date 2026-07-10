package in.bachatsetu.backend.admin.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.admin.application.port.ClockPort;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

class AdminInfrastructureConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AdminInfrastructureConfig.class);

    @Test
    void wiresEveryPortAdapterWhenATransactionManagerIsAvailable() {
        contextRunner
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ClockPort.class);
                    assertThat(context).hasSingleBean(TransactionPort.class);
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
                });
    }
}
