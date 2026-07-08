package in.bachatsetu.backend.automation.interfaces.scheduler.config;

import in.bachatsetu.backend.automation.application.port.ClockPort;
import in.bachatsetu.backend.automation.application.service.DrawSchedulerApplicationService;
import in.bachatsetu.backend.automation.application.service.OverdueReminderApplicationService;
import in.bachatsetu.backend.automation.application.service.PaymentDueReminderApplicationService;
import in.bachatsetu.backend.automation.application.service.ReceiptCleanupApplicationService;
import in.bachatsetu.backend.automation.application.usecase.RunDrawSchedulerUseCase;
import in.bachatsetu.backend.automation.application.usecase.RunOverdueReminderUseCase;
import in.bachatsetu.backend.automation.application.usecase.RunPaymentDueReminderUseCase;
import in.bachatsetu.backend.automation.application.usecase.RunReceiptCleanupUseCase;
import in.bachatsetu.backend.automation.domain.port.InstallmentReminderRepository;
import in.bachatsetu.backend.draw.application.usecase.ConductDrawUseCase;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.domain.port.GroupRepository;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes the four Automation use cases. Each depends only on pre-existing ports and pre-existing use
 * cases from other modules — {@link ConductDrawUseCase} and {@link CreateNotificationUseCase} in
 * particular are called exactly as they would be from any other caller, never bypassed.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled}, matching every other module's
 * application config, since every bean here depends transitively on repository-backed beans.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AutomationApplicationConfig {

    @Bean
    public RunDrawSchedulerUseCase runDrawSchedulerUseCase(
            DrawRepository drawRepository,
            GroupRepository groupRepository,
            ConductDrawUseCase conductDraw,
            ClockPort clock) {
        return new DrawSchedulerApplicationService(drawRepository, groupRepository, conductDraw, clock);
    }

    @Bean
    public RunPaymentDueReminderUseCase runPaymentDueReminderUseCase(
            InstallmentReminderRepository installmentRepository,
            NotificationRepository notificationRepository,
            CreateNotificationUseCase createNotification,
            UserRepository userRepository,
            ClockPort clock,
            @Value("${bachatsetu.automation.payment-reminder.days-ahead:3}") int lookAheadDays) {
        return new PaymentDueReminderApplicationService(
                installmentRepository, notificationRepository, createNotification, userRepository, clock,
                lookAheadDays);
    }

    @Bean
    public RunOverdueReminderUseCase runOverdueReminderUseCase(
            InstallmentReminderRepository installmentRepository,
            NotificationRepository notificationRepository,
            CreateNotificationUseCase createNotification,
            UserRepository userRepository,
            ClockPort clock) {
        return new OverdueReminderApplicationService(
                installmentRepository, notificationRepository, createNotification, userRepository, clock);
    }

    @Bean
    public RunReceiptCleanupUseCase runReceiptCleanupUseCase() {
        return new ReceiptCleanupApplicationService();
    }
}
