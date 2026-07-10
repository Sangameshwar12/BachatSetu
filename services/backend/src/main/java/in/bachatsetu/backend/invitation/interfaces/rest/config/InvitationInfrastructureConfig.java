package in.bachatsetu.backend.invitation.interfaces.rest.config;

import in.bachatsetu.backend.invitation.application.port.ClockPort;
import in.bachatsetu.backend.invitation.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.invitation.application.port.InvitationCodeGeneratorPort;
import in.bachatsetu.backend.invitation.application.port.InvitationTokenGeneratorPort;
import in.bachatsetu.backend.invitation.application.port.TransactionPort;
import in.bachatsetu.backend.invitation.interfaces.rest.adapter.ApplicationEventInvitationEventPublisherAdapter;
import in.bachatsetu.backend.invitation.interfaces.rest.adapter.RandomInvitationCodeGeneratorAdapter;
import in.bachatsetu.backend.invitation.interfaces.rest.adapter.RandomInvitationTokenGeneratorAdapter;
import in.bachatsetu.backend.invitation.interfaces.rest.adapter.SpringInvitationTransactionAdapter;
import in.bachatsetu.backend.invitation.interfaces.rest.adapter.SystemInvitationClockAdapter;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Composes the invitation module's outbound port adapters.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled} for the same reason documented on
 * {@code NotificationInfrastructureConfig}.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class InvitationInfrastructureConfig {

    @Bean
    Clock invitationClock() {
        return Clock.systemUTC();
    }

    @Bean
    SecureRandom invitationSecureRandom() {
        return new SecureRandom();
    }

    @Bean
    TransactionTemplate invitationTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    ClockPort systemInvitationClockAdapter(Clock invitationClock) {
        return new SystemInvitationClockAdapter(invitationClock);
    }

    @Bean
    TransactionPort springInvitationTransactionAdapter(TransactionTemplate invitationTransactionTemplate) {
        return new SpringInvitationTransactionAdapter(invitationTransactionTemplate);
    }

    @Bean
    DomainEventPublisherPort applicationEventInvitationEventPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventInvitationEventPublisherAdapter(publisher);
    }

    @Bean
    InvitationCodeGeneratorPort randomInvitationCodeGeneratorAdapter(SecureRandom invitationSecureRandom) {
        return new RandomInvitationCodeGeneratorAdapter(invitationSecureRandom);
    }

    @Bean
    InvitationTokenGeneratorPort randomInvitationTokenGeneratorAdapter(SecureRandom invitationSecureRandom) {
        return new RandomInvitationTokenGeneratorAdapter(invitationSecureRandom);
    }
}
