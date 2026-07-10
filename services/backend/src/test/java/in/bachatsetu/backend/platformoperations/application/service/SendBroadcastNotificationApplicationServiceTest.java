package in.bachatsetu.backend.platformoperations.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.platformoperations.application.command.SendBroadcastNotificationCommand;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.application.query.BroadcastResult;
import in.bachatsetu.backend.platformoperations.domain.model.BroadcastRecipient;
import in.bachatsetu.backend.platformoperations.domain.model.BroadcastScope;
import in.bachatsetu.backend.platformoperations.domain.port.BroadcastRecipientRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class SendBroadcastNotificationApplicationServiceTest {

    private final BroadcastRecipientRepository recipientRepository = mock(BroadcastRecipientRepository.class);
    private final CreateNotificationUseCase createNotification = mock(CreateNotificationUseCase.class);
    private final CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);
    private final TransactionPort transaction = new StubTransactionPort();
    private final SendBroadcastNotificationApplicationService service =
            new SendBroadcastNotificationApplicationService(
                    recipientRepository, createNotification, createAuditEntry, transaction);

    @Test
    void sendsOneNotificationPerRecipient() {
        BroadcastRecipient recipientA = new BroadcastRecipient(AggregateId.newId(), AggregateId.newId());
        BroadcastRecipient recipientB = new BroadcastRecipient(AggregateId.newId(), AggregateId.newId());
        when(recipientRepository.resolve(BroadcastScope.ALL_USERS, null)).thenReturn(List.of(recipientA, recipientB));

        BroadcastResult result = service.execute(new SendBroadcastNotificationCommand(
                BroadcastScope.ALL_USERS, null, "Title", "Message", AggregateId.newId()));

        assertThat(result.recipientCount()).isEqualTo(2);
        assertThat(result.sentCount()).isEqualTo(2);
        assertThat(result.failedCount()).isEqualTo(0);
        verify(createNotification, org.mockito.Mockito.times(2)).execute(any(CreateNotificationCommand.class));
        verify(createAuditEntry).execute(any());
    }

    @Test
    void continuesTheBroadcastWhenOneRecipientFails() {
        BroadcastRecipient recipientA = new BroadcastRecipient(AggregateId.newId(), AggregateId.newId());
        BroadcastRecipient recipientB = new BroadcastRecipient(AggregateId.newId(), AggregateId.newId());
        when(recipientRepository.resolve(BroadcastScope.ALL_USERS, null)).thenReturn(List.of(recipientA, recipientB));
        when(createNotification.execute(any()))
                .thenThrow(new RuntimeException("delivery failed"))
                .thenReturn(null);

        BroadcastResult result = service.execute(new SendBroadcastNotificationCommand(
                BroadcastScope.ALL_USERS, null, "Title", "Message", AggregateId.newId()));

        assertThat(result.recipientCount()).isEqualTo(2);
        assertThat(result.sentCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
    }

    private static final class StubTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
