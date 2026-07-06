package in.bachatsetu.backend.member.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.member.application.port.ClockPort;
import in.bachatsetu.backend.member.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.member.application.port.MemberNumberGeneratorPort;
import in.bachatsetu.backend.member.application.port.TransactionPort;
import in.bachatsetu.backend.member.interfaces.rest.adapter.ApplicationEventMemberEventPublisherAdapter;
import in.bachatsetu.backend.member.interfaces.rest.adapter.MemberNumberGeneratorAdapter;
import in.bachatsetu.backend.member.interfaces.rest.adapter.SpringMemberTransactionAdapter;
import in.bachatsetu.backend.member.interfaces.rest.adapter.SystemMemberClockAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

class MemberInfrastructureConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MemberInfrastructureConfig.class);

    @Test
    void wiresEveryPortWhenTransactionManagerIsAvailable() {
        contextRunner
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(ClockPort.class).isInstanceOf(SystemMemberClockAdapter.class);
                    assertThat(context).getBean(TransactionPort.class)
                            .isInstanceOf(SpringMemberTransactionAdapter.class);
                    assertThat(context).getBean(MemberNumberGeneratorPort.class)
                            .isInstanceOf(MemberNumberGeneratorAdapter.class);
                    assertThat(context).getBean(DomainEventPublisherPort.class)
                            .isInstanceOf(ApplicationEventMemberEventPublisherAdapter.class);
                });
    }

    @Test
    void doesNotWireAdaptersWithoutATransactionManager() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ClockPort.class);
            assertThat(context).doesNotHaveBean(TransactionPort.class);
            assertThat(context).doesNotHaveBean(MemberNumberGeneratorPort.class);
            assertThat(context).doesNotHaveBean(DomainEventPublisherPort.class);
        });
    }
}
