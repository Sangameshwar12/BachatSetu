package in.bachatsetu.backend.automation.interfaces.scheduler.config;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.automation.application.port.ClockPort;
import in.bachatsetu.backend.automation.interfaces.scheduler.adapter.SystemAutomationClockAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AutomationInfrastructureConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AutomationInfrastructureConfig.class);

    @Test
    void wiresTheClockPortByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).getBean(ClockPort.class).isInstanceOf(SystemAutomationClockAdapter.class);
        });
    }

    @Test
    void doesNotWireTheClockPortWhenPersistenceRepositoriesDisabled() {
        contextRunner
                .withPropertyValues("bachatsetu.persistence.repositories.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ClockPort.class);
                });
    }
}
