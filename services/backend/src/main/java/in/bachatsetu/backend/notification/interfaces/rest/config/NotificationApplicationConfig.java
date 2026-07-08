package in.bachatsetu.backend.notification.interfaces.rest.config;

import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.notification.application.mapper.NotificationApplicationMapper;
import in.bachatsetu.backend.notification.application.port.ClockPort;
import in.bachatsetu.backend.notification.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.notification.application.port.EmailSender;
import in.bachatsetu.backend.notification.application.port.InAppNotificationSender;
import in.bachatsetu.backend.notification.application.port.SmsSender;
import in.bachatsetu.backend.notification.application.port.TransactionPort;
import in.bachatsetu.backend.notification.application.port.WhatsappSender;
import in.bachatsetu.backend.notification.application.service.CreateNotificationApplicationService;
import in.bachatsetu.backend.notification.application.service.GetNotificationApplicationService;
import in.bachatsetu.backend.notification.application.service.ListNotificationsApplicationService;
import in.bachatsetu.backend.notification.application.service.MarkNotificationDeliveredApplicationService;
import in.bachatsetu.backend.notification.application.service.MarkNotificationFailedApplicationService;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.application.usecase.GetNotificationUseCase;
import in.bachatsetu.backend.notification.application.usecase.ListNotificationsUseCase;
import in.bachatsetu.backend.notification.application.usecase.MarkNotificationDeliveredUseCase;
import in.bachatsetu.backend.notification.application.usecase.MarkNotificationFailedUseCase;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.notification.domain.service.NotificationTemplateRenderer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes framework-free Notification application services when all outbound ports exist.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled} rather than a
 * cross-configuration-class {@code @ConditionalOnBean} check: regular (non-auto-configuration)
 * {@code @Configuration} classes discovered by component scanning have no guaranteed processing
 * order relative to one another, so a class-level {@code @ConditionalOnBean} referencing ports
 * defined by {@code NotificationInfrastructureConfig} was evaluated non-deterministically and could
 * skip this configuration even when every required port was actually present.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class NotificationApplicationConfig {

    @Bean
    public NotificationApplicationMapper notificationApplicationMapper() {
        return new NotificationApplicationMapper();
    }

    @Bean
    public CreateNotificationUseCase createNotificationUseCase(
            NotificationRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            NotificationApplicationMapper mapper,
            NotificationTemplateRenderer renderer,
            EmailSender emailSender,
            SmsSender smsSender,
            WhatsappSender whatsappSender,
            InAppNotificationSender inAppNotificationSender,
            CreateAuditEntryUseCase createAuditEntry) {
        return new CreateNotificationApplicationService(
                repository,
                eventPublisher,
                clock,
                transaction,
                mapper,
                renderer,
                emailSender,
                smsSender,
                whatsappSender,
                inAppNotificationSender,
                createAuditEntry);
    }

    @Bean
    public GetNotificationUseCase getNotificationUseCase(
            NotificationRepository repository,
            TransactionPort transaction,
            NotificationApplicationMapper mapper) {
        return new GetNotificationApplicationService(repository, transaction, mapper);
    }

    @Bean
    public ListNotificationsUseCase listNotificationsUseCase(
            NotificationRepository repository,
            TransactionPort transaction,
            NotificationApplicationMapper mapper) {
        return new ListNotificationsApplicationService(repository, transaction, mapper);
    }

    @Bean
    public MarkNotificationDeliveredUseCase markNotificationDeliveredUseCase(
            NotificationRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            NotificationApplicationMapper mapper) {
        return new MarkNotificationDeliveredApplicationService(repository, eventPublisher, clock, transaction, mapper);
    }

    @Bean
    public MarkNotificationFailedUseCase markNotificationFailedUseCase(
            NotificationRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            NotificationApplicationMapper mapper) {
        return new MarkNotificationFailedApplicationService(repository, eventPublisher, clock, transaction, mapper);
    }
}
