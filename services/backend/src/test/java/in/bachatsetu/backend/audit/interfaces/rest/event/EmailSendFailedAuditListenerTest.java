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
import in.bachatsetu.backend.email.application.event.EmailSendFailed;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailSendFailedAuditListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-11T08:00:00Z");

    private CreateAuditEntryUseCase createAuditEntry;
    private EmailSendFailedAuditListener listener;

    @BeforeEach
    void setUp() {
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        listener = new EmailSendFailedAuditListener(createAuditEntry);
    }

    @Test
    void recordsAnEmailSendFailedEntryWithTheProviderNameAndFailureReasonAsMetadata() {
        EmailSendFailed event = new EmailSendFailed(
                UUID.randomUUID(), "so****@example.com", EmailTemplateCategory.INVITATION, "SENDGRID",
                "SendGrid request failed with HTTP 503", 17, NOW);

        listener.onEmailSendFailed(event);

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        CreateAuditEntryCommand command = captor.getValue();
        assertThat(command.eventType()).isEqualTo(AuditEventType.EMAIL_SEND_FAILED);
        assertThat(command.moduleName()).isEqualTo("email");
        assertThat(command.metadata()).contains("SENDGRID").contains("HTTP 503");
    }

    @Test
    void swallowsAnAuditFailureWithoutPropagating() {
        EmailSendFailed event = new EmailSendFailed(
                UUID.randomUUID(), "so****@example.com", EmailTemplateCategory.INVITATION, "SENDGRID",
                "network error", 5, NOW);
        when(createAuditEntry.execute(any())).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> listener.onEmailSendFailed(event)).doesNotThrowAnyException();
    }
}
