package in.bachatsetu.backend.member.interfaces.rest.config;

import in.bachatsetu.backend.member.application.port.ClockPort;
import in.bachatsetu.backend.member.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.member.application.port.MemberNumberGeneratorPort;
import in.bachatsetu.backend.member.application.port.TransactionPort;
import in.bachatsetu.backend.member.domain.service.MemberNumberGenerator;
import in.bachatsetu.backend.member.interfaces.rest.adapter.ApplicationEventMemberEventPublisherAdapter;
import in.bachatsetu.backend.member.interfaces.rest.adapter.MemberNumberGeneratorAdapter;
import in.bachatsetu.backend.member.interfaces.rest.adapter.SpringMemberTransactionAdapter;
import in.bachatsetu.backend.member.interfaces.rest.adapter.SystemMemberClockAdapter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Composes the Member outbound port adapters backing the application layer.
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
public class MemberInfrastructureConfig {

    @Bean
    Clock memberClock() {
        return Clock.systemUTC();
    }

    @Bean
    MemberNumberGenerator memberNumberGenerator() {
        return new MemberNumberGenerator();
    }

    @Bean
    TransactionTemplate memberTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    ClockPort systemMemberClockAdapter(Clock memberClock) {
        return new SystemMemberClockAdapter(memberClock);
    }

    @Bean
    TransactionPort springMemberTransactionAdapter(TransactionTemplate memberTransactionTemplate) {
        return new SpringMemberTransactionAdapter(memberTransactionTemplate);
    }

    @Bean
    MemberNumberGeneratorPort memberNumberGeneratorAdapter(MemberNumberGenerator memberNumberGenerator) {
        return new MemberNumberGeneratorAdapter(memberNumberGenerator);
    }

    @Bean
    DomainEventPublisherPort applicationEventMemberEventPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventMemberEventPublisherAdapter(publisher);
    }
}
