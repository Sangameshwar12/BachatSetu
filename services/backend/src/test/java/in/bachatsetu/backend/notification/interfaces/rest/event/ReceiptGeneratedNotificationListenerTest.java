package in.bachatsetu.backend.notification.interfaces.rest.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.receipt.domain.event.ReceiptGenerated;
import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.model.ReceiptDescription;
import in.bachatsetu.backend.receipt.domain.model.ReceiptLine;
import in.bachatsetu.backend.receipt.domain.model.ReceiptNumber;
import in.bachatsetu.backend.receipt.domain.model.ReceiptType;
import in.bachatsetu.backend.receipt.domain.port.ReceiptRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReceiptGeneratedNotificationListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private ReceiptRepository receiptRepository;
    private CreateNotificationUseCase createNotification;
    private ReceiptGeneratedNotificationListener listener;

    @BeforeEach
    void setUp() {
        receiptRepository = mock(ReceiptRepository.class);
        createNotification = mock(CreateNotificationUseCase.class);
        listener = new ReceiptGeneratedNotificationListener(receiptRepository, createNotification);
    }

    @Test
    void notifiesTheMemberThatTheirReceiptIsAvailable() {
        Receipt receipt = newReceipt();
        when(receiptRepository.findById(receipt.id())).thenReturn(Optional.of(receipt));

        listener.onReceiptGenerated(new ReceiptGenerated(
                UUID.randomUUID(), receipt.id(), receipt.paymentId(), receipt.number(), receipt.total(), NOW));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotification).execute(captor.capture());
        CreateNotificationCommand command = captor.getValue();
        assertThat(command.tenantId()).isEqualTo(receipt.tenantId());
        assertThat(command.recipientUserId()).isEqualTo(receipt.memberId());
        assertThat(command.channel()).isEqualTo(NotificationChannel.PUSH);
        assertThat(command.category()).isEqualTo(NotificationCategory.RECEIPT);
        assertThat(command.placeholders()).containsEntry("title", "Receipt Available");
        assertThat(command.placeholders()).containsEntry("body", "Your payment receipt is ready for download.");
    }

    @Test
    void doesNothingWhenTheReceiptNoLongerExists() {
        AggregateId receiptId = AggregateId.newId();
        when(receiptRepository.findById(receiptId)).thenReturn(Optional.empty());

        listener.onReceiptGenerated(new ReceiptGenerated(
                UUID.randomUUID(), receiptId, AggregateId.newId(), new ReceiptNumber("RCT/20260707/00000001"),
                Money.inr(500_000), NOW));

        verify(createNotification, never()).execute(any());
    }

    @Test
    void swallowsAndLogsANotificationFailureWithoutRethrowing() {
        Receipt receipt = newReceipt();
        when(receiptRepository.findById(receipt.id())).thenReturn(Optional.of(receipt));
        when(createNotification.execute(any())).thenThrow(new RuntimeException("dispatch failed"));

        assertThatCode(() -> listener.onReceiptGenerated(new ReceiptGenerated(
                        UUID.randomUUID(), receipt.id(), receipt.paymentId(), receipt.number(), receipt.total(), NOW)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNullConstructorArguments() {
        assertThatThrownBy(() -> new ReceiptGeneratedNotificationListener(null, createNotification))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ReceiptGeneratedNotificationListener(receiptRepository, null))
                .isInstanceOf(NullPointerException.class);
    }

    private Receipt newReceipt() {
        List<ReceiptLine> lines = List.of(new ReceiptLine(
                AggregateId.newId(), ReceiptType.CONTRIBUTION,
                new ReceiptDescription("Monthly contribution"), Money.inr(500_000)));
        return Receipt.generate(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                new ReceiptNumber("RCT/20260707/00000001"), lines, AggregateId.newId(), NOW);
    }
}
