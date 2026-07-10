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
import in.bachatsetu.backend.auth.domain.event.UserRegistered;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.Email;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SignupAuditListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    private CreateAuditEntryUseCase createAuditEntry;
    private SignupAuditListener listener;

    @BeforeEach
    void setUp() {
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        listener = new SignupAuditListener(createAuditEntry);
    }

    @Test
    void recordsAUserRegisteredEntry() {
        UserId userId = UserId.newId();
        UserRegistered event = new UserRegistered(
                UUID.randomUUID(), userId, new Email("asha@example.com"), MobileNumber.of("+919876543210"), NOW);

        listener.onUserRegistered(event);

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        CreateAuditEntryCommand command = captor.getValue();
        assertThat(command.tenantId()).isNull();
        assertThat(command.actorId()).isEqualTo(userId.toAggregateId());
        assertThat(command.eventType()).isEqualTo(AuditEventType.USER_REGISTERED);
        assertThat(command.moduleName()).isEqualTo("auth");
    }

    @Test
    void swallowsAnAuditFailureWithoutPropagating() {
        UserRegistered event = new UserRegistered(
                UUID.randomUUID(), UserId.newId(), new Email("asha@example.com"), MobileNumber.of("+919876543210"),
                NOW);
        when(createAuditEntry.execute(any())).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> listener.onUserRegistered(event)).doesNotThrowAnyException();
    }
}
