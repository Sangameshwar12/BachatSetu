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
import in.bachatsetu.backend.email.application.event.EmailSent;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailSentAuditListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-11T08:00:00Z");

    private CreateAuditEntryUseCase createAuditEntry;
    private EmailSentAuditListener listener;

    @BeforeEach
    void setUp() {
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        listener = new EmailSentAuditListener(createAuditEntry);
    }

    @Test
    void recordsAnEmailSentEntryWithProviderDurationAndMessageIdAsMetadata() {
        EmailSent event = new EmailSent(
                UUID.randomUUID(), "so****@example.com", EmailTemplateCategory.WELCOME, "RESEND", "msg-1", 42, NOW);

        listener.onEmailSent(event);

        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        CreateAuditEntryCommand command = captor.getValue();
        assertThat(command.tenantId()).isNull();
        assertThat(command.eventType()).isEqualTo(AuditEventType.EMAIL_SENT);
        assertThat(command.moduleName()).isEqualTo("email");
        assertThat(command.metadata()).contains("RESEND").contains("msg-1").contains("42");
    }

    @Test
    void swallowsAnAuditFailureWithoutPropagating() {
        EmailSent event = new EmailSent(
                UUID.randomUUID(), "so****@example.com", EmailTemplateCategory.WELCOME, "RESEND", "msg-1", 42, NOW);
        when(createAuditEntry.execute(any())).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> listener.onEmailSent(event)).doesNotThrowAnyException();
    }
}
