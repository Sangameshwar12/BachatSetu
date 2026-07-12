package in.bachatsetu.backend.audit.interfaces.rest.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.auth.domain.event.RefreshTokenRevoked;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.UserId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LogoutAuditListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-12T08:00:00Z");

    private CreateAuditEntryUseCase createAuditEntry;
    private LogoutAuditListener listener;

    @BeforeEach
    void setUp() {
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        listener = new LogoutAuditListener(createAuditEntry);
    }

    @Test
    void recordsALogoutEntryWhenARefreshTokenIsRevoked() {
        UserId userId = UserId.newId();
        RefreshTokenRevoked event = new RefreshTokenRevoked(UUID.randomUUID(), RefreshTokenId.newId(), userId, NOW);

        listener.onRefreshTokenRevoked(event);

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        CreateAuditEntryCommand command = captor.getValue();
        assertThat(command.tenantId()).isNull();
        assertThat(command.actorId()).isEqualTo(userId.toAggregateId());
        assertThat(command.eventType()).isEqualTo(AuditEventType.LOGOUT);
        assertThat(command.moduleName()).isEqualTo("auth");
    }

    @Test
    void swallowsAnAuditFailureWithoutPropagating() {
        RefreshTokenRevoked event =
                new RefreshTokenRevoked(UUID.randomUUID(), RefreshTokenId.newId(), UserId.newId(), NOW);
        when(createAuditEntry.execute(any())).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> listener.onRefreshTokenRevoked(event)).doesNotThrowAnyException();
    }
}
