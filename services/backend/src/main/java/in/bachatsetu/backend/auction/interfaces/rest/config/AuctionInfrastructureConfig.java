package in.bachatsetu.backend.auction.interfaces.rest.config;

import in.bachatsetu.backend.auction.application.port.ClockPort;
import in.bachatsetu.backend.auction.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auction.application.port.TransactionPort;
import in.bachatsetu.backend.auction.interfaces.rest.adapter.ApplicationEventAuctionEventPublisherAdapter;
import in.bachatsetu.backend.auction.interfaces.rest.adapter.SpringAuctionTransactionAdapter;
import in.bachatsetu.backend.auction.interfaces.rest.adapter.SystemAuctionClockAdapter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Composes the Auction outbound port adapters backing the application layer.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled} rather than
 * {@code @ConditionalOnBean(PlatformTransactionManager.class)}: {@code PlatformTransactionManager}
 * is registered by a Spring Boot auto-configuration, which is processed as a deferred import
 * after every regular, component-scanned {@code @Configuration} class has already had its
 * class-level conditions evaluated — so the bean-presence check was never guaranteed to see it.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuctionInfrastructureConfig {

    @Bean
    Clock auctionClock() {
        return Clock.systemUTC();
    }

    @Bean
    TransactionTemplate auctionTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    ClockPort systemAuctionClockAdapter(Clock auctionClock) {
        return new SystemAuctionClockAdapter(auctionClock);
    }

    @Bean
    TransactionPort springAuctionTransactionAdapter(TransactionTemplate auctionTransactionTemplate) {
        return new SpringAuctionTransactionAdapter(auctionTransactionTemplate);
    }

    @Bean
    DomainEventPublisherPort applicationEventAuctionEventPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventAuctionEventPublisherAdapter(publisher);
    }
}
