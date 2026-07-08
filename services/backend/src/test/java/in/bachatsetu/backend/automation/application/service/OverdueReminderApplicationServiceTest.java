package in.bachatsetu.backend.automation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.automation.application.port.ClockPort;
import in.bachatsetu.backend.automation.application.query.JobRunResult;
import in.bachatsetu.backend.automation.domain.model.DueInstallment;
import in.bachatsetu.backend.automation.domain.port.InstallmentReminderRepository;
import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.query.NotificationResult;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OverdueReminderApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 8);

    private InstallmentReminderRepository installmentRepository;
    private NotificationRepository notificationRepository;
    private CreateNotificationUseCase createNotification;
    private UserRepository userRepository;
    private ClockPort clock;
    private OverdueReminderApplicationService service;

    @BeforeEach
    void setUp() {
        installmentRepository = mock(InstallmentReminderRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        createNotification = mock(CreateNotificationUseCase.class);
        userRepository = mock(UserRepository.class);
        clock = mock(ClockPort.class);
        when(clock.now()).thenReturn(NOW);
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        service = new OverdueReminderApplicationService(
                installmentRepository, notificationRepository, createNotification, userRepository, clock);
    }

    @Test
    void sendsAnOverdueReminderUsingThePaymentPassthroughCategory() {
        DueInstallment installment = newOverdueInstallment();
        when(installmentRepository.findOverdueBefore(TODAY)).thenReturn(List.of(installment));
        when(notificationRepository.existsForRecipientSince(any(), any(), any(), any())).thenReturn(false);
        when(createNotification.execute(any())).thenReturn(newNotificationResult());

        JobRunResult result = service.execute();

        assertThat(result.processedCount()).isEqualTo(1);
        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotification).execute(captor.capture());
        CreateNotificationCommand command = captor.getValue();
        assertThat(command.category()).isEqualTo(NotificationCategory.PAYMENT);
        assertThat(command.placeholders()).containsEntry("title", "Payment Overdue");
        assertThat(command.placeholders().get("body")).contains("overdue");
    }

    @Test
    void skipsARecipientAlreadyRemindedToday() {
        DueInstallment installment = newOverdueInstallment();
        when(installmentRepository.findOverdueBefore(TODAY)).thenReturn(List.of(installment));
        when(notificationRepository.existsForRecipientSince(
                        eq(installment.tenantId()), eq(installment.recipientUserId()),
                        eq(NotificationCategory.PAYMENT), any()))
                .thenReturn(true);

        JobRunResult result = service.execute();

        assertThat(result.processedCount()).isZero();
        assertThat(result.skippedCount()).isEqualTo(1);
        verify(createNotification, never()).execute(any());
    }

    @Test
    void returnsAnEmptyResultWhenNothingIsOverdue() {
        when(installmentRepository.findOverdueBefore(TODAY)).thenReturn(List.of());

        JobRunResult result = service.execute();

        assertThat(result).isEqualTo(JobRunResult.empty());
    }

    @Test
    void continuesTheRunWhenSendingOneReminderFails() {
        DueInstallment first = newOverdueInstallment();
        DueInstallment second = newOverdueInstallment();
        when(installmentRepository.findOverdueBefore(TODAY)).thenReturn(List.of(first, second));
        when(notificationRepository.existsForRecipientSince(any(), any(), any(), any())).thenReturn(false);
        when(createNotification.execute(any()))
                .thenThrow(new RuntimeException("dispatch failed"))
                .thenReturn(newNotificationResult());

        JobRunResult result = service.execute();

        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
    }

    @Test
    void rejectsNullConstructorArguments() {
        assertThatThrownBy(() -> new OverdueReminderApplicationService(
                        null, notificationRepository, createNotification, userRepository, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OverdueReminderApplicationService(
                        installmentRepository, notificationRepository, createNotification, userRepository, null))
                .isInstanceOf(NullPointerException.class);
    }

    private DueInstallment newOverdueInstallment() {
        return new DueInstallment(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                "Sunrise Bhishi", 50_000L, "INR", TODAY.minusDays(5));
    }

    private NotificationResult newNotificationResult() {
        return new NotificationResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "member@example.com", "PUSH",
                "PAYMENT", "Payment Overdue", "body", "SENT", NOW, NOW, NOW, null, null, 1);
    }
}
