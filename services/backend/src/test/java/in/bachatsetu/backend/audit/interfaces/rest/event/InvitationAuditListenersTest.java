package in.bachatsetu.backend.audit.interfaces.rest.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.invitation.domain.event.InvitationAccepted;
import in.bachatsetu.backend.invitation.domain.event.InvitationCreated;
import in.bachatsetu.backend.invitation.domain.event.InvitationRevoked;
import in.bachatsetu.backend.invitation.domain.model.InvitationType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InvitationAuditListenersTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    @Test
    void recordsInvitationCreated() {
        CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);
        InvitationCreatedAuditListener listener = new InvitationCreatedAuditListener(createAuditEntry);
        AggregateId invitationId = AggregateId.newId();
        AggregateId groupId = AggregateId.newId();

        listener.onInvitationCreated(new InvitationCreated(UUID.randomUUID(), invitationId, groupId, NOW));

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.INVITATION_CREATED);
        assertThat(captor.getValue().actorId()).isEqualTo(groupId);
    }

    @Test
    void recordsInvitationRevoked() {
        CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);
        InvitationRevokedAuditListener listener = new InvitationRevokedAuditListener(createAuditEntry);

        listener.onInvitationRevoked(
                new InvitationRevoked(UUID.randomUUID(), AggregateId.newId(), AggregateId.newId(), NOW));

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.INVITATION_REVOKED);
    }

    @Test
    void recordsGroupJoinedForCodeChannel() {
        CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);
        InvitationAcceptedAuditListener listener = new InvitationAcceptedAuditListener(createAuditEntry);

        listener.onInvitationAccepted(new InvitationAccepted(
                UUID.randomUUID(), AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                InvitationType.CODE, NOW));

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.GROUP_JOINED);
    }

    @Test
    void recordsQrJoinedForQrChannel() {
        CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);
        InvitationAcceptedAuditListener listener = new InvitationAcceptedAuditListener(createAuditEntry);

        listener.onInvitationAccepted(new InvitationAccepted(
                UUID.randomUUID(), AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                InvitationType.QR, NOW));

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.QR_JOINED);
    }

    @Test
    void recordsLinkJoinedForLinkChannel() {
        CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);
        InvitationAcceptedAuditListener listener = new InvitationAcceptedAuditListener(createAuditEntry);

        listener.onInvitationAccepted(new InvitationAccepted(
                UUID.randomUUID(), AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                InvitationType.LINK, NOW));

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.LINK_JOINED);
    }

    @Test
    void swallowsAuditFailuresWithoutPropagating() {
        CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);
        org.mockito.Mockito.when(createAuditEntry.execute(any())).thenThrow(new RuntimeException("boom"));
        InvitationCreatedAuditListener listener = new InvitationCreatedAuditListener(createAuditEntry);

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> listener.onInvitationCreated(
                new InvitationCreated(UUID.randomUUID(), AggregateId.newId(), AggregateId.newId(), NOW))))
                .isNull();
    }
}
