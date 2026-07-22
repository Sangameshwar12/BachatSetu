package in.bachatsetu.backend.notification.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.email.application.port.EmailSenderPort;
import in.bachatsetu.backend.notification.application.port.ClockPort;
import in.bachatsetu.backend.notification.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.notification.application.port.EmailSender;
import in.bachatsetu.backend.notification.application.port.InAppNotificationSender;
import in.bachatsetu.backend.notification.application.port.SmsSender;
import in.bachatsetu.backend.notification.application.port.TransactionPort;
import in.bachatsetu.backend.notification.application.port.WhatsappSender;
import in.bachatsetu.backend.notification.domain.service.NotificationTemplateRenderer;
import in.bachatsetu.backend.notification.interfaces.rest.adapter.ApplicationEventNotificationEventPublisherAdapter;
import in.bachatsetu.backend.notification.interfaces.rest.adapter.LoggingInAppNotificationSenderAdapter;
import in.bachatsetu.backend.notification.interfaces.rest.adapter.LoggingSmsSenderAdapter;
import in.bachatsetu.backend.notification.interfaces.rest.adapter.LoggingWhatsappSenderAdapter;
import in.bachatsetu.backend.notification.interfaces.rest.adapter.RealEmailSenderAdapter;
import in.bachatsetu.backend.notification.interfaces.rest.adapter.SpringNotificationTransactionAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

class NotificationInfrastructureConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(NotificationInfrastructureConfig.class);

    @Test
    void wiresEveryPortWhenTransactionManagerIsAvailable() {
        contextRunner
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .withBean(EmailSenderPort.class, () -> mock(EmailSenderPort.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(ClockPort.class).isNotNull();
                    assertThat(context).getBean(TransactionPort.class)
                            .isInstanceOf(SpringNotificationTransactionAdapter.class);
                    assertThat(context).getBean(DomainEventPublisherPort.class)
                            .isInstanceOf(ApplicationEventNotificationEventPublisherAdapter.class);
                    assertThat(context).getBean(NotificationTemplateRenderer.class).isNotNull();
                    assertThat(context).getBean(EmailSender.class).isInstanceOf(RealEmailSenderAdapter.class);
                    assertThat(context).getBean(SmsSender.class).isInstanceOf(LoggingSmsSenderAdapter.class);
                    assertThat(context).getBean(WhatsappSender.class).isInstanceOf(LoggingWhatsappSenderAdapter.class);
                    assertThat(context).getBean(InAppNotificationSender.class)
                            .isInstanceOf(LoggingInAppNotificationSenderAdapter.class);
                });
    }

    @Test
    void doesNotWireAdaptersWhenPersistenceRepositoriesDisabled() {
        contextRunner
                .withPropertyValues("bachatsetu.persistence.repositories.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(TransactionPort.class);
                    assertThat(context).doesNotHaveBean(DomainEventPublisherPort.class);
                    assertThat(context).doesNotHaveBean(EmailSender.class);
                    assertThat(context).doesNotHaveBean(SmsSender.class);
                    assertThat(context).doesNotHaveBean(WhatsappSender.class);
                    assertThat(context).doesNotHaveBean(InAppNotificationSender.class);
                });
    }
}
