package in.bachatsetu.backend.notification.interfaces.rest.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.group.domain.GroupDomainFixtures;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.group.domain.port.GroupRepository;
import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.payment.domain.event.PaymentStatusChanged;
import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentMethod;
import in.bachatsetu.backend.payment.domain.model.PaymentReference;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.payment.domain.model.ProviderReference;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PaymentVerifiedNotificationListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private PaymentRepository paymentRepository;
    private GroupRepository groupRepository;
    private CreateNotificationUseCase createNotification;
    private PaymentVerifiedNotificationListener listener;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        groupRepository = mock(GroupRepository.class);
        createNotification = mock(CreateNotificationUseCase.class);
        listener = new PaymentVerifiedNotificationListener(paymentRepository, groupRepository, createNotification);
    }

    @Test
    void notifiesTheMemberAndTheOrganizerWhenPaymentBecomesVerified() {
        AggregateId organizerId = AggregateId.newId();
        Payment payment = newVerifiedPayment();
        SavingsGroup group = GroupDomainFixtures.newGroup(organizerId, 5);
        when(paymentRepository.findById(payment.id())).thenReturn(Optional.of(payment));
        when(groupRepository.findById(payment.groupId())).thenReturn(Optional.of(group));

        listener.onPaymentStatusChanged(changedEvent(payment));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotification, times(2)).execute(captor.capture());
        List<CreateNotificationCommand> commands = captor.getAllValues();

        CreateNotificationCommand memberCommand = commands.get(0);
        assertThat(memberCommand.tenantId()).isEqualTo(payment.tenantId());
        assertThat(memberCommand.recipientUserId()).isEqualTo(payment.memberId());
        assertThat(memberCommand.category()).isEqualTo(NotificationCategory.PAYMENT);
        assertThat(memberCommand.placeholders()).containsEntry("title", "Payment Received");

        CreateNotificationCommand organizerCommand = commands.get(1);
        assertThat(organizerCommand.recipientUserId()).isEqualTo(organizerId);
        assertThat(organizerCommand.category()).isEqualTo(NotificationCategory.PAYMENT);
    }

    @Test
    void doesNotNotifyTheOrganizerSeparatelyWhenTheOrganizerMadeThePayment() {
        Payment payment = newVerifiedPayment();
        SavingsGroup group = GroupDomainFixtures.newGroup(payment.memberId(), 5);
        when(paymentRepository.findById(payment.id())).thenReturn(Optional.of(payment));
        when(groupRepository.findById(payment.groupId())).thenReturn(Optional.of(group));

        listener.onPaymentStatusChanged(changedEvent(payment));

        verify(createNotification, times(1)).execute(any());
    }

    @Test
    void ignoresTransitionsToStatusesOtherThanVerified() {
        Payment payment = newVerifiedPayment();

        listener.onPaymentStatusChanged(new PaymentStatusChanged(
                UUID.randomUUID(), payment.id(), PaymentStatus.INITIATED, PaymentStatus.FAILED, NOW));

        verify(createNotification, never()).execute(any());
        verify(paymentRepository, never()).findById(any());
    }

    @Test
    void doesNothingWhenThePaymentNoLongerExists() {
        AggregateId paymentId = AggregateId.newId();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        listener.onPaymentStatusChanged(new PaymentStatusChanged(
                UUID.randomUUID(), paymentId, PaymentStatus.INITIATED, PaymentStatus.VERIFIED, NOW));

        verify(createNotification, never()).execute(any());
    }

    @Test
    void swallowsAndLogsANotificationFailureWithoutRethrowing() {
        Payment payment = newVerifiedPayment();
        when(paymentRepository.findById(payment.id())).thenReturn(Optional.of(payment));
        when(groupRepository.findById(payment.groupId())).thenReturn(Optional.empty());
        when(createNotification.execute(any())).thenThrow(new RuntimeException("dispatch failed"));

        assertThatCode(() -> listener.onPaymentStatusChanged(changedEvent(payment))).doesNotThrowAnyException();
    }

    @Test
    void rejectsNullConstructorArguments() {
        assertThatThrownBy(() -> new PaymentVerifiedNotificationListener(null, groupRepository, createNotification))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PaymentVerifiedNotificationListener(paymentRepository, null, createNotification))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PaymentVerifiedNotificationListener(paymentRepository, groupRepository, null))
                .isInstanceOf(NullPointerException.class);
    }

    private PaymentStatusChanged changedEvent(Payment payment) {
        return new PaymentStatusChanged(
                UUID.randomUUID(), payment.id(), PaymentStatus.INITIATED, PaymentStatus.VERIFIED, NOW);
    }

    private Payment newVerifiedPayment() {
        Payment payment = Payment.initiate(
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                new PaymentReference("PAY-12345678"),
                new IdempotencyKey("checkout-attempt-0001"),
                Money.inr(100_000),
                PaymentMethod.UPI,
                AggregateId.newId(),
                NOW);
        payment.verify(new ProviderReference("test-provider", "txn-001"), payment.tenantId(), NOW.plusSeconds(60));
        return payment;
    }
}
