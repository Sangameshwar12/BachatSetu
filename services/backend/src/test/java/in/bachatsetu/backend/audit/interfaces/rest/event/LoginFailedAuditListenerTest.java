package in.bachatsetu.backend.audit.interfaces.rest.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.auth.application.event.OtpExpired;
import in.bachatsetu.backend.auth.application.event.OtpRejected;
import in.bachatsetu.backend.auth.application.event.OtpRejectionReason;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LoginFailedAuditListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-12T08:00:00Z");

    private CreateAuditEntryUseCase createAuditEntry;
    private LoginFailedAuditListener listener;

    @BeforeEach
    void setUp() {
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        listener = new LoginFailedAuditListener(createAuditEntry);
    }

    @Test
    void recordsALoginFailedEntryWhenASignInOtpIsRejected() {
        UserId userId = UserId.newId();
        OtpRejected event = new OtpRejected(
                UUID.randomUUID(), AggregateId.newId(), userId, OtpPurpose.SIGN_IN,
                OtpRejectionReason.INVALID_CODE, NOW);

        listener.onOtpRejected(event);

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        assertThatCommandRecordsALoginFailure(captor.getValue(), userId);
    }

    @Test
    void recordsALoginFailedEntryWhenASignInOtpExpires() {
        UserId userId = UserId.newId();
        OtpExpired event = new OtpExpired(UUID.randomUUID(), AggregateId.newId(), userId, OtpPurpose.SIGN_IN, NOW);

        listener.onOtpExpired(event);

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        assertThatCommandRecordsALoginFailure(captor.getValue(), userId);
    }

    @Test
    void ignoresARegistrationOtpRejection() {
        OtpRejected event = new OtpRejected(
                UUID.randomUUID(), AggregateId.newId(), UserId.newId(), OtpPurpose.REGISTRATION,
                OtpRejectionReason.INVALID_CODE, NOW);

        listener.onOtpRejected(event);

        verify(createAuditEntry, never()).execute(any());
    }

    @Test
    void swallowsAnAuditFailureWithoutPropagating() {
        OtpRejected event = new OtpRejected(
                UUID.randomUUID(), AggregateId.newId(), UserId.newId(), OtpPurpose.SIGN_IN,
                OtpRejectionReason.INVALID_CODE, NOW);
        when(createAuditEntry.execute(any())).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> listener.onOtpRejected(event)).doesNotThrowAnyException();
    }

    private void assertThatCommandRecordsALoginFailure(CreateAuditEntryCommand command, UserId userId) {
        assertThat(command.tenantId()).isNull();
        assertThat(command.actorId()).isEqualTo(userId.toAggregateId());
        assertThat(command.eventType()).isEqualTo(AuditEventType.LOGIN_FAILED);
        assertThat(command.moduleName()).isEqualTo("auth");
    }
}
