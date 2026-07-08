package in.bachatsetu.backend.notification.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.notification.domain.service.NotificationTemplateRenderer;
import org.junit.jupiter.api.Test;

class NotificationApplicationConfigTest {

    private final NotificationApplicationConfig config = new NotificationApplicationConfig();
    private final NotificationApplicationMapper mapper = config.notificationApplicationMapper();
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final DomainEventPublisherPort eventPublisher = mock(DomainEventPublisherPort.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = mock(TransactionPort.class);
    private final NotificationTemplateRenderer renderer = new NotificationTemplateRenderer();
    private final EmailSender emailSender = mock(EmailSender.class);
    private final SmsSender smsSender = mock(SmsSender.class);
    private final WhatsappSender whatsappSender = mock(WhatsappSender.class);
    private final InAppNotificationSender inAppNotificationSender = mock(InAppNotificationSender.class);

    @Test
    void composesCreateNotificationUseCase() {
        assertThat(config.createNotificationUseCase(
                        repository, eventPublisher, clock, transaction, mapper, renderer,
                        emailSender, smsSender, whatsappSender, inAppNotificationSender))
                .isInstanceOf(CreateNotificationApplicationService.class);
    }

    @Test
    void composesGetNotificationUseCase() {
        assertThat(config.getNotificationUseCase(repository, transaction, mapper))
                .isInstanceOf(GetNotificationApplicationService.class);
    }

    @Test
    void composesListNotificationsUseCase() {
        assertThat(config.listNotificationsUseCase(repository, transaction, mapper))
                .isInstanceOf(ListNotificationsApplicationService.class);
    }

    @Test
    void composesMarkNotificationDeliveredUseCase() {
        assertThat(config.markNotificationDeliveredUseCase(repository, eventPublisher, clock, transaction, mapper))
                .isInstanceOf(MarkNotificationDeliveredApplicationService.class);
    }

    @Test
    void composesMarkNotificationFailedUseCase() {
        assertThat(config.markNotificationFailedUseCase(repository, eventPublisher, clock, transaction, mapper))
                .isInstanceOf(MarkNotificationFailedApplicationService.class);
    }
}
