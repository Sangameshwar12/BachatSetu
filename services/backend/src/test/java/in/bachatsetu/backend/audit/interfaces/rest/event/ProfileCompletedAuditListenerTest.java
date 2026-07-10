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
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.user.domain.event.ProfileCompleted;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProfileCompletedAuditListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    private CreateAuditEntryUseCase createAuditEntry;
    private ProfileCompletedAuditListener listener;

    @BeforeEach
    void setUp() {
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        listener = new ProfileCompletedAuditListener(createAuditEntry);
    }

    @Test
    void recordsAProfileCompletedEntry() {
        AggregateId profileId = AggregateId.newId();
        ProfileCompleted event = new ProfileCompleted(UUID.randomUUID(), profileId, NOW);

        listener.onProfileCompleted(event);

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        CreateAuditEntryCommand command = captor.getValue();
        assertThat(command.actorId()).isEqualTo(profileId);
        assertThat(command.eventType()).isEqualTo(AuditEventType.PROFILE_COMPLETED);
        assertThat(command.moduleName()).isEqualTo("user");
    }

    @Test
    void swallowsAnAuditFailureWithoutPropagating() {
        ProfileCompleted event = new ProfileCompleted(UUID.randomUUID(), AggregateId.newId(), NOW);
        when(createAuditEntry.execute(any())).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> listener.onProfileCompleted(event)).doesNotThrowAnyException();
    }
}
