package in.bachatsetu.backend.notification.interfaces.rest.config;

import in.bachatsetu.backend.notification.application.port.ClockPort;
import in.bachatsetu.backend.notification.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.notification.application.port.EmailSender;
import in.bachatsetu.backend.notification.application.port.InAppNotificationSender;
import in.bachatsetu.backend.notification.application.port.SmsSender;
import in.bachatsetu.backend.notification.application.port.TransactionPort;
import in.bachatsetu.backend.notification.application.port.WhatsappSender;
import in.bachatsetu.backend.notification.domain.service.NotificationTemplateRenderer;
import in.bachatsetu.backend.notification.interfaces.rest.adapter.ApplicationEventNotificationEventPublisherAdapter;
import in.bachatsetu.backend.notification.interfaces.rest.adapter.LoggingEmailSenderAdapter;
import in.bachatsetu.backend.notification.interfaces.rest.adapter.LoggingInAppNotificationSenderAdapter;
import in.bachatsetu.backend.notification.interfaces.rest.adapter.LoggingSmsSenderAdapter;
import in.bachatsetu.backend.notification.interfaces.rest.adapter.LoggingWhatsappSenderAdapter;
import in.bachatsetu.backend.notification.interfaces.rest.adapter.SpringNotificationTransactionAdapter;
import in.bachatsetu.backend.notification.interfaces.rest.adapter.SystemNotificationClockAdapter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Composes the Notification outbound port adapters backing the application layer.
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
public class NotificationInfrastructureConfig {

    @Bean
    Clock notificationClock() {
        return Clock.systemUTC();
    }

    @Bean
    NotificationTemplateRenderer notificationTemplateRenderer() {
        return new NotificationTemplateRenderer();
    }

    @Bean
    TransactionTemplate notificationTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    ClockPort systemNotificationClockAdapter(Clock notificationClock) {
        return new SystemNotificationClockAdapter(notificationClock);
    }

    @Bean
    TransactionPort springNotificationTransactionAdapter(TransactionTemplate notificationTransactionTemplate) {
        return new SpringNotificationTransactionAdapter(notificationTransactionTemplate);
    }

    @Bean
    DomainEventPublisherPort applicationEventNotificationEventPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventNotificationEventPublisherAdapter(publisher);
    }

    @Bean
    EmailSender loggingEmailSenderAdapter() {
        return new LoggingEmailSenderAdapter();
    }

    @Bean
    SmsSender loggingSmsSenderAdapter() {
        return new LoggingSmsSenderAdapter();
    }

    @Bean
    WhatsappSender loggingWhatsappSenderAdapter() {
        return new LoggingWhatsappSenderAdapter();
    }

    @Bean
    InAppNotificationSender loggingInAppNotificationSenderAdapter() {
        return new LoggingInAppNotificationSenderAdapter();
    }
}
