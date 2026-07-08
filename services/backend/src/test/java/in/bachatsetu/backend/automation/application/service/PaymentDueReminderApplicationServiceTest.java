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
import in.bachatsetu.backend.shared.domain.Email;
import in.bachatsetu.backend.user.domain.model.PersonName;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import in.bachatsetu.backend.user.domain.model.UserContact;
import in.bachatsetu.backend.user.domain.model.UserProfile;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PaymentDueReminderApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 8);

    private InstallmentReminderRepository installmentRepository;
    private NotificationRepository notificationRepository;
    private CreateNotificationUseCase createNotification;
    private UserRepository userRepository;
    private ClockPort clock;
    private PaymentDueReminderApplicationService service;

    @BeforeEach
    void setUp() {
        installmentRepository = mock(InstallmentReminderRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        createNotification = mock(CreateNotificationUseCase.class);
        userRepository = mock(UserRepository.class);
        clock = mock(ClockPort.class);
        when(clock.now()).thenReturn(NOW);
        service = new PaymentDueReminderApplicationService(
                installmentRepository, notificationRepository, createNotification, userRepository, clock, 3);
    }

    @Test
    void sendsAContributionReminderForEachDueInstallment() {
        DueInstallment installment = newDueInstallment();
        when(installmentRepository.findDueBetween(TODAY, TODAY.plusDays(3))).thenReturn(List.of(installment));
        when(notificationRepository.existsForRecipientSince(any(), any(), any(), any())).thenReturn(false);
        when(userRepository.findById(installment.recipientUserId()))
                .thenReturn(Optional.of(newUser("Asha", "Rao")));
        when(createNotification.execute(any())).thenReturn(newNotificationResult());

        JobRunResult result = service.execute();

        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotification).execute(captor.capture());
        CreateNotificationCommand command = captor.getValue();
        assertThat(command.category()).isEqualTo(NotificationCategory.CONTRIBUTION_REMINDER);
        assertThat(command.tenantId()).isEqualTo(installment.tenantId());
        assertThat(command.recipientUserId()).isEqualTo(installment.recipientUserId());
        assertThat(command.placeholders()).containsEntry("memberName", "Asha Rao");
        assertThat(command.placeholders()).containsEntry("groupName", installment.groupName());
    }

    @Test
    void usesADefaultDisplayNameWhenTheUserCannotBeFound() {
        DueInstallment installment = newDueInstallment();
        when(installmentRepository.findDueBetween(TODAY, TODAY.plusDays(3))).thenReturn(List.of(installment));
        when(notificationRepository.existsForRecipientSince(any(), any(), any(), any())).thenReturn(false);
        when(userRepository.findById(installment.recipientUserId())).thenReturn(Optional.empty());
        when(createNotification.execute(any())).thenReturn(newNotificationResult());

        service.execute();

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotification).execute(captor.capture());
        assertThat(captor.getValue().placeholders()).containsEntry("memberName", "Member");
    }

    @Test
    void skipsARecipientAlreadyRemindedToday() {
        DueInstallment installment = newDueInstallment();
        when(installmentRepository.findDueBetween(TODAY, TODAY.plusDays(3))).thenReturn(List.of(installment));
        when(notificationRepository.existsForRecipientSince(
                        eq(installment.tenantId()), eq(installment.recipientUserId()),
                        eq(NotificationCategory.CONTRIBUTION_REMINDER), any()))
                .thenReturn(true);

        JobRunResult result = service.execute();

        assertThat(result.processedCount()).isZero();
        assertThat(result.skippedCount()).isEqualTo(1);
        verify(createNotification, never()).execute(any());
    }

    @Test
    void returnsAnEmptyResultWhenNothingIsDue() {
        when(installmentRepository.findDueBetween(TODAY, TODAY.plusDays(3))).thenReturn(List.of());

        JobRunResult result = service.execute();

        assertThat(result).isEqualTo(JobRunResult.empty());
    }

    @Test
    void continuesTheRunWhenSendingOneReminderFails() {
        DueInstallment first = newDueInstallment();
        DueInstallment second = newDueInstallment();
        when(installmentRepository.findDueBetween(TODAY, TODAY.plusDays(3))).thenReturn(List.of(first, second));
        when(notificationRepository.existsForRecipientSince(any(), any(), any(), any())).thenReturn(false);
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(createNotification.execute(any()))
                .thenThrow(new RuntimeException("dispatch failed"))
                .thenReturn(newNotificationResult());

        JobRunResult result = service.execute();

        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
    }

    @Test
    void rejectsNullConstructorArgumentsAndNegativeLookAhead() {
        assertThatThrownBy(() -> new PaymentDueReminderApplicationService(
                        null, notificationRepository, createNotification, userRepository, clock, 3))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PaymentDueReminderApplicationService(
                        installmentRepository, notificationRepository, createNotification, userRepository, null, 3))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PaymentDueReminderApplicationService(
                        installmentRepository, notificationRepository, createNotification, userRepository, clock, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private DueInstallment newDueInstallment() {
        return new DueInstallment(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                "Sunrise Bhishi", 50_000L, "INR", TODAY.plusDays(2));
    }

    private UserProfile newUser(String givenName, String familyName) {
        return UserProfile.register(
                AggregateId.newId(), new PersonName(givenName, familyName),
                new UserContact(new Email("member@example.com"), null),
                PreferredLanguage.ENGLISH, AggregateId.newId(), NOW);
    }

    private NotificationResult newNotificationResult() {
        return new NotificationResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "member@example.com", "PUSH",
                "CONTRIBUTION_REMINDER", "Contribution reminder", "body", "SENT", NOW, NOW, NOW, null, null, 1);
    }
}
