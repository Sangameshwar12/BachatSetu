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
import in.bachatsetu.backend.auth.application.event.OtpSendFailed;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OtpSendFailedAuditListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-11T08:00:00Z");

    private CreateAuditEntryUseCase createAuditEntry;
    private OtpSendFailedAuditListener listener;

    @BeforeEach
    void setUp() {
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        listener = new OtpSendFailedAuditListener(createAuditEntry);
    }

    @Test
    void recordsAnOtpSendFailedEntryWithTheProviderNameAndFailureReasonAsMetadata() {
        UserId userId = UserId.newId();
        OtpSendFailed event = new OtpSendFailed(
                UUID.randomUUID(), userId, OtpPurpose.SIGN_IN, "MSG91",
                "MSG91 request failed with HTTP 503", NOW);

        listener.onOtpSendFailed(event);

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        CreateAuditEntryCommand command = captor.getValue();
        assertThat(command.tenantId()).isNull();
        assertThat(command.actorId()).isEqualTo(userId.toAggregateId());
        assertThat(command.eventType()).isEqualTo(AuditEventType.OTP_SEND_FAILED);
        assertThat(command.moduleName()).isEqualTo("auth");
        assertThat(command.metadata()).contains("MSG91").contains("HTTP 503");
    }

    @Test
    void swallowsAnAuditFailureWithoutPropagating() {
        OtpSendFailed event = new OtpSendFailed(
                UUID.randomUUID(), UserId.newId(), OtpPurpose.SIGN_IN, "TWILIO", "network error", NOW);
        when(createAuditEntry.execute(any())).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> listener.onOtpSendFailed(event)).doesNotThrowAnyException();
    }
}
