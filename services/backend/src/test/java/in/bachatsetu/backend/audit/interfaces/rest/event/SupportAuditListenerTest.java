package in.bachatsetu.backend.audit.interfaces.rest.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.support.domain.event.SupportTicketAssigned;
import in.bachatsetu.backend.support.domain.event.SupportTicketClosed;
import in.bachatsetu.backend.support.domain.event.SupportTicketCreated;
import in.bachatsetu.backend.support.domain.event.SupportTicketResolved;
import in.bachatsetu.backend.support.domain.model.TicketCategory;
import in.bachatsetu.backend.support.domain.model.TicketPriority;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SupportAuditListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    @Test
    void recordsTicketCreated() {
        CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);
        SupportAuditListener listener = new SupportAuditListener(createAuditEntry);
        AggregateId tenantId = AggregateId.newId();
        AggregateId ticketId = AggregateId.newId();

        listener.onTicketCreated(new SupportTicketCreated(
                UUID.randomUUID(), ticketId, tenantId, TicketCategory.PAYMENT, TicketPriority.HIGH, NOW));

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.SUPPORT_TICKET_CREATED);
        assertThat(captor.getValue().tenantId()).isEqualTo(tenantId);
    }

    @Test
    void recordsTicketAssigned() {
        CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);
        SupportAuditListener listener = new SupportAuditListener(createAuditEntry);
        AggregateId assignedTo = AggregateId.newId();

        listener.onTicketAssigned(new SupportTicketAssigned(UUID.randomUUID(), AggregateId.newId(), assignedTo, NOW));

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.SUPPORT_TICKET_ASSIGNED);
        assertThat(captor.getValue().actorId()).isEqualTo(assignedTo);
    }

    @Test
    void recordsTicketResolved() {
        CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);
        SupportAuditListener listener = new SupportAuditListener(createAuditEntry);

        listener.onTicketResolved(new SupportTicketResolved(UUID.randomUUID(), AggregateId.newId(), NOW));

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.SUPPORT_TICKET_RESOLVED);
    }

    @Test
    void recordsTicketClosed() {
        CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);
        SupportAuditListener listener = new SupportAuditListener(createAuditEntry);

        listener.onTicketClosed(new SupportTicketClosed(UUID.randomUUID(), AggregateId.newId(), NOW));

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.SUPPORT_TICKET_CLOSED);
    }

    @Test
    void swallowsAFailureWhileRecordingTicketCreated() {
        CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);
        doThrow(new RuntimeException("boom")).when(createAuditEntry).execute(org.mockito.ArgumentMatchers.any());
        SupportAuditListener listener = new SupportAuditListener(createAuditEntry);

        listener.onTicketCreated(new SupportTicketCreated(
                UUID.randomUUID(), AggregateId.newId(), AggregateId.newId(), TicketCategory.OTHER,
                TicketPriority.LOW, NOW));
    }
}
