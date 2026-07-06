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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Composes the Member outbound port adapters backing the application layer. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(PlatformTransactionManager.class)
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
