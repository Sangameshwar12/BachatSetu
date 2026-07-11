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
import in.bachatsetu.backend.auth.application.event.OtpSent;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OtpSentAuditListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-11T08:00:00Z");

    private CreateAuditEntryUseCase createAuditEntry;
    private OtpSentAuditListener listener;

    @BeforeEach
    void setUp() {
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        listener = new OtpSentAuditListener(createAuditEntry);
    }

    @Test
    void recordsAnOtpSentEntryForEveryPurpose() {
        UserId userId = UserId.newId();
        OtpSent event = new OtpSent(UUID.randomUUID(), AggregateId.newId(), userId, OtpPurpose.SIGN_IN, NOW);

        listener.onOtpSent(event);

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        CreateAuditEntryCommand command = captor.getValue();
        assertThat(command.tenantId()).isNull();
        assertThat(command.actorId()).isEqualTo(userId.toAggregateId());
        assertThat(command.eventType()).isEqualTo(AuditEventType.OTP_SENT);
        assertThat(command.moduleName()).isEqualTo("auth");
    }

    @Test
    void swallowsAnAuditFailureWithoutPropagating() {
        OtpSent event = new OtpSent(
                UUID.randomUUID(), AggregateId.newId(), UserId.newId(), OtpPurpose.REGISTRATION, NOW);
        when(createAuditEntry.execute(any())).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> listener.onOtpSent(event)).doesNotThrowAnyException();
    }
}
