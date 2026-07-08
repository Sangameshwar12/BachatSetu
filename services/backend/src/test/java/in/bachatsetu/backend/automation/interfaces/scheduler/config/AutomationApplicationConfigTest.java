package in.bachatsetu.backend.automation.interfaces.scheduler.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.automation.application.port.ClockPort;
import in.bachatsetu.backend.automation.application.service.DrawSchedulerApplicationService;
import in.bachatsetu.backend.automation.application.service.OverdueReminderApplicationService;
import in.bachatsetu.backend.automation.application.service.PaymentDueReminderApplicationService;
import in.bachatsetu.backend.automation.application.service.ReceiptCleanupApplicationService;
import in.bachatsetu.backend.automation.domain.port.InstallmentReminderRepository;
import in.bachatsetu.backend.draw.application.usecase.ConductDrawUseCase;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.domain.port.GroupRepository;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import org.junit.jupiter.api.Test;

class AutomationApplicationConfigTest {

    private final AutomationApplicationConfig config = new AutomationApplicationConfig();
    private final DrawRepository drawRepository = mock(DrawRepository.class);
    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final ConductDrawUseCase conductDraw = mock(ConductDrawUseCase.class);
    private final InstallmentReminderRepository installmentRepository = mock(InstallmentReminderRepository.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final CreateNotificationUseCase createNotification = mock(CreateNotificationUseCase.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ClockPort clock = mock(ClockPort.class);

    @Test
    void composesRunDrawSchedulerUseCase() {
        assertThat(config.runDrawSchedulerUseCase(drawRepository, groupRepository, conductDraw, clock))
                .isInstanceOf(DrawSchedulerApplicationService.class);
    }

    @Test
    void composesRunPaymentDueReminderUseCase() {
        assertThat(config.runPaymentDueReminderUseCase(
                        installmentRepository, notificationRepository, createNotification, userRepository, clock, 3))
                .isInstanceOf(PaymentDueReminderApplicationService.class);
    }

    @Test
    void composesRunOverdueReminderUseCase() {
        assertThat(config.runOverdueReminderUseCase(
                        installmentRepository, notificationRepository, createNotification, userRepository, clock))
                .isInstanceOf(OverdueReminderApplicationService.class);
    }

    @Test
    void composesRunReceiptCleanupUseCase() {
        assertThat(config.runReceiptCleanupUseCase()).isInstanceOf(ReceiptCleanupApplicationService.class);
    }
}
