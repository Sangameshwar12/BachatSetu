package in.bachatsetu.backend.audit.interfaces.rest.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.auth.application.event.OtpVerified;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LoginAuditListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private CreateAuditEntryUseCase createAuditEntry;
    private LoginAuditListener listener;

    @BeforeEach
    void setUp() {
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        listener = new LoginAuditListener(createAuditEntry);
    }

    @Test
    void recordsALoginEntryForASignInOtpVerification() {
        UserId userId = UserId.newId();
        OtpVerified event = new OtpVerified(UUID.randomUUID(), AggregateId.newId(), userId, OtpPurpose.SIGN_IN, NOW);

        listener.onOtpVerified(event);

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        CreateAuditEntryCommand command = captor.getValue();
        assertThatCommandRecordsALogin(command, userId);
    }

    @Test
    void ignoresARegistrationOtpVerification() {
        OtpVerified event = new OtpVerified(
                UUID.randomUUID(), AggregateId.newId(), UserId.newId(), OtpPurpose.REGISTRATION, NOW);

        listener.onOtpVerified(event);

        verify(createAuditEntry, never()).execute(any());
    }

    @Test
    void swallowsAnAuditFailureWithoutPropagating() {
        UserId userId = UserId.newId();
        OtpVerified event = new OtpVerified(UUID.randomUUID(), AggregateId.newId(), userId, OtpPurpose.SIGN_IN, NOW);
        when(createAuditEntry.execute(any())).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> listener.onOtpVerified(event)).doesNotThrowAnyException();
    }

    private void assertThatCommandRecordsALogin(CreateAuditEntryCommand command, UserId userId) {
        org.assertj.core.api.Assertions.assertThat(command.tenantId()).isNull();
        org.assertj.core.api.Assertions.assertThat(command.actorId()).isEqualTo(userId.toAggregateId());
        org.assertj.core.api.Assertions.assertThat(command.eventType()).isEqualTo(AuditEventType.LOGIN);
        org.assertj.core.api.Assertions.assertThat(command.moduleName()).isEqualTo("auth");
    }
}
