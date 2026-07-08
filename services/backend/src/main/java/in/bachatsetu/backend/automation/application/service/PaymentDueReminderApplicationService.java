package in.bachatsetu.backend.automation.application.service;

import in.bachatsetu.backend.automation.application.port.ClockPort;
import in.bachatsetu.backend.automation.application.query.JobRunResult;
import in.bachatsetu.backend.automation.application.usecase.RunPaymentDueReminderUseCase;
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
 * Queues a reminder for every outstanding contribution due within the configured look-ahead window,
 * reusing the pre-existing {@link NotificationCategory#CONTRIBUTION_REMINDER} category and its fixed
 * template ("... your contribution of {{amount}} to {{groupName}} is due soon.") — this is an exact fit for
 * this job's purpose, so no new category or template was introduced. Never sends a channel directly; every
 * notification goes through {@link CreateNotificationUseCase}, exactly as every other event-driven
 * notification in this codebase does.
 */
public final class PaymentDueReminderApplicationService implements RunPaymentDueReminderUseCase {

    private final InstallmentReminderRepository installmentRepository;
    private final int lookAheadDays;
    private final ReminderApplicationSupport support;

    public PaymentDueReminderApplicationService(
            InstallmentReminderRepository installmentRepository,
            NotificationRepository notificationRepository,
            CreateNotificationUseCase createNotification,
            UserRepository userRepository,
            ClockPort clock,
            int lookAheadDays) {
        this.installmentRepository =
                Objects.requireNonNull(installmentRepository, "installment repository must not be null");
        if (lookAheadDays < 0) {
            throw new IllegalArgumentException("lookAheadDays must not be negative");
        }
        this.lookAheadDays = lookAheadDays;
        this.support = new ReminderApplicationSupport(
                notificationRepository, createNotification, userRepository,
                Objects.requireNonNull(clock, "clock must not be null"));
    }

    @Override
    public JobRunResult execute() {
        LocalDate today = support.today();
        LocalDate windowEnd = today.plusDays(lookAheadDays);
        List<DueInstallment> dueInstallments = installmentRepository.findDueBetween(today, windowEnd);
        return support.remind(dueInstallments, NotificationCategory.CONTRIBUTION_REMINDER, this::placeholders);
    }

    private Map<String, String> placeholders(DueInstallment installment, String memberName) {
        return Map.of(
                "memberName", memberName,
                "amount", ReminderApplicationSupport.formatAmount(installment),
                "groupName", installment.groupName());
    }
}
