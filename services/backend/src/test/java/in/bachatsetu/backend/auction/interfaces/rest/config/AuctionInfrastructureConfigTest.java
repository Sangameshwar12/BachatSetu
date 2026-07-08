package in.bachatsetu.backend.auction.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.auction.application.port.ClockPort;
import in.bachatsetu.backend.auction.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auction.application.port.TransactionPort;
import in.bachatsetu.backend.auction.interfaces.rest.adapter.ApplicationEventAuctionEventPublisherAdapter;
import in.bachatsetu.backend.auction.interfaces.rest.adapter.SpringAuctionTransactionAdapter;
import in.bachatsetu.backend.auction.interfaces.rest.adapter.SystemAuctionClockAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

class AuctionInfrastructureConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AuctionInfrastructureConfig.class);

    @Test
    void wiresEveryPortWhenTransactionManagerIsAvailable() {
        contextRunner
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(ClockPort.class).isInstanceOf(SystemAuctionClockAdapter.class);
                    assertThat(context).getBean(TransactionPort.class)
                            .isInstanceOf(SpringAuctionTransactionAdapter.class);
                    assertThat(context).getBean(DomainEventPublisherPort.class)
                            .isInstanceOf(ApplicationEventAuctionEventPublisherAdapter.class);
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
                });
    }
}
