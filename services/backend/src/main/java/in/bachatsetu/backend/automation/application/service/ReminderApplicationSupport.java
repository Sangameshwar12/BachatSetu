package in.bachatsetu.backend.automation.application.service;

import in.bachatsetu.backend.automation.application.port.ClockPort;
import in.bachatsetu.backend.automation.application.query.JobRunResult;
import in.bachatsetu.backend.automation.domain.model.DueInstallment;
import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.user.domain.model.PersonName;
import in.bachatsetu.backend.user.domain.model.UserProfile;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared mechanics for the two installment-reminder jobs (due-soon and overdue): resolving a display name,
 * skipping a recipient already reminded today, dispatching through the pre-existing
 * {@link CreateNotificationUseCase}, and counting outcomes. Each job supplies its own
 * {@link NotificationCategory} and placeholder content; the dedup and dispatch mechanics are identical.
 *
 * <p>Never logs directly — application code must not depend on a logging framework (see
 * {@code LayerDependencyArchitectureTest}) — so failures are collected as plain messages in the returned
 * {@link JobRunResult} for the calling {@code interfaces.scheduler} job to log.
 */
final class ReminderApplicationSupport {

    private static final String DEFAULT_MEMBER_NAME = "Member";

    private final NotificationRepository notificationRepository;
    private final CreateNotificationUseCase createNotification;
    private final UserRepository userRepository;
    private final ClockPort clock;

    ReminderApplicationSupport(
            NotificationRepository notificationRepository,
            CreateNotificationUseCase createNotification,
            UserRepository userRepository,
            ClockPort clock) {
        this.notificationRepository =
                Objects.requireNonNull(notificationRepository, "notification repository must not be null");
        this.createNotification = Objects.requireNonNull(createNotification, "create notification must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "user repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    LocalDate today() {
        return clock.now().atZone(ZoneOffset.UTC).toLocalDate();
    }

    JobRunResult remind(
            List<DueInstallment> installments, NotificationCategory category, PlaceholderBuilder placeholders) {
        Instant since = today().atStartOfDay(ZoneOffset.UTC).toInstant();
        int processed = 0;
        int skipped = 0;
        List<String> failures = new ArrayList<>();
        for (DueInstallment installment : installments) {
            if (notificationRepository.existsForRecipientSince(
                    installment.tenantId(), installment.recipientUserId(), category, since)) {
                skipped++;
                continue;
            }
            String failure = trySendReminder(installment, category, placeholders);
            if (failure == null) {
                processed++;
            } else {
                failures.add(failure);
            }
        }
        return new JobRunResult(processed, skipped, failures);
    }

    private String trySendReminder(
            DueInstallment installment, NotificationCategory category, PlaceholderBuilder builder) {
        try {
            sendReminder(installment, category, builder);
            return null;
        } catch (RuntimeException exception) {
            return "Failed to send a reminder for installment " + installment.installmentId()
                    + ": " + exception.getMessage();
        }
    }

    private void sendReminder(DueInstallment installment, NotificationCategory category, PlaceholderBuilder builder) {
        String memberName = displayNameOf(installment);
        createNotification.execute(new CreateNotificationCommand(
                installment.tenantId(),
                installment.recipientUserId(),
                installment.recipientUserId().value().toString(),
                NotificationChannel.PUSH,
                category,
                builder.build(installment, memberName),
                installment.recipientUserId()));
    }

    private String displayNameOf(DueInstallment installment) {
        return userRepository.findById(installment.recipientUserId())
                .map(UserProfile::name)
                .map(PersonName::displayName)
                .orElse(DEFAULT_MEMBER_NAME);
    }

    static String formatAmount(DueInstallment installment) {
        BigDecimal amount = BigDecimal.valueOf(installment.outstandingAmountPaise(), 2);
        DecimalFormat format = new DecimalFormat("#,##0.00");
        return format.format(amount) + " " + installment.currencyCode();
    }

    @FunctionalInterface
    interface PlaceholderBuilder {
        Map<String, String> build(DueInstallment installment, String memberName);
    }
}
