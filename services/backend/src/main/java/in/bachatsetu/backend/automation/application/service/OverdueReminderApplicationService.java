package in.bachatsetu.backend.automation.application.service;

import in.bachatsetu.backend.automation.application.port.ClockPort;
import in.bachatsetu.backend.automation.application.query.JobRunResult;
import in.bachatsetu.backend.automation.application.usecase.RunOverdueReminderUseCase;
import in.bachatsetu.backend.automation.domain.model.DueInstallment;
import in.bachatsetu.backend.automation.domain.port.InstallmentReminderRepository;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Queues a reminder for every outstanding contribution whose due date has already passed. No pre-existing
 * {@link NotificationCategory} fits "overdue" wording (unlike the due-soon job, which reuses {@code
 * CONTRIBUTION_REMINDER} directly), so this reuses the pass-through {@link NotificationCategory#PAYMENT}
 * category introduced in Sprint 11.9, supplying the exact "Payment Overdue" wording as {@code title}/{@code
 * body} placeholders rather than introducing a seventh category.
 *
 * <p>Same-day de-duplication (via {@link NotificationRepository#existsForRecipientSince}) is what stops an
 * installment that is overdue for multiple consecutive days from generating more than one reminder per day
 * — it does not stop one reminder per day for as long as the installment remains unpaid, which mirrors how
 * a genuinely overdue reminder is expected to behave.
 */
public final class OverdueReminderApplicationService implements RunOverdueReminderUseCase {

    private final InstallmentReminderRepository installmentRepository;
    private final ReminderApplicationSupport support;

    public OverdueReminderApplicationService(
            InstallmentReminderRepository installmentRepository,
            NotificationRepository notificationRepository,
            CreateNotificationUseCase createNotification,
            UserRepository userRepository,
            ClockPort clock) {
        this.installmentRepository =
                Objects.requireNonNull(installmentRepository, "installment repository must not be null");
        this.support = new ReminderApplicationSupport(
                notificationRepository, createNotification, userRepository,
                Objects.requireNonNull(clock, "clock must not be null"));
    }

    @Override
    public JobRunResult execute() {
        LocalDate today = support.today();
        List<DueInstallment> overdueInstallments = installmentRepository.findOverdueBefore(today);
        return support.remind(overdueInstallments, NotificationCategory.PAYMENT, this::placeholders);
    }

    private Map<String, String> placeholders(DueInstallment installment, String memberName) {
        String body = memberName + ", your contribution of " + ReminderApplicationSupport.formatAmount(installment)
                + " to " + installment.groupName() + " was due on " + installment.dueDate() + " and is overdue.";
        return Map.of("title", "Payment Overdue", "body", body);
    }
}
