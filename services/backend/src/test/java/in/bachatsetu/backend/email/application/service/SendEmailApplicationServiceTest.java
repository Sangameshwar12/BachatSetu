package in.bachatsetu.backend.email.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.email.application.command.SendEmailCommand;
import in.bachatsetu.backend.email.application.port.EmailSenderPort;
import in.bachatsetu.backend.email.domain.model.EmailAddress;
import in.bachatsetu.backend.email.domain.model.EmailDeliveryStatus;
import in.bachatsetu.backend.email.domain.model.EmailMessage;
import in.bachatsetu.backend.email.domain.model.EmailSendResult;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import in.bachatsetu.backend.email.domain.service.EmailTemplateCatalog;
import in.bachatsetu.backend.email.domain.service.EmailTemplateRenderer;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SendEmailApplicationServiceTest {

    private final EmailTemplateCatalog catalog = new EmailTemplateCatalog();
    private final EmailTemplateRenderer renderer = new EmailTemplateRenderer();

    @Test
    void rendersTheTemplateAndPassesTheResultToThePort() {
        AtomicReference<EmailMessage> captured = new AtomicReference<>();
        EmailSenderPort port = message -> {
            captured.set(message);
            return new EmailSendResult(EmailDeliveryStatus.SENT, "RESEND", "msg-1", Instant.now(), null);
        };
        SendEmailApplicationService service = new SendEmailApplicationService(port, catalog, renderer);

        EmailSendResult result = service.execute(new SendEmailCommand(
                new EmailAddress("organizer@example.com"), EmailTemplateCategory.INVITATION,
                Map.of("groupName", "Diwali Bachat", "invitationCode", "ABC123")));

        assertThat(result.status()).isEqualTo(EmailDeliveryStatus.SENT);
        assertThat(captured.get().to().value()).isEqualTo("organizer@example.com");
        assertThat(captured.get().category()).isEqualTo(EmailTemplateCategory.INVITATION);
        assertThat(captured.get().content().subject()).contains("Diwali Bachat");
        assertThat(captured.get().content().htmlBody()).contains("ABC123");
    }

    @Test
    void propagatesTheFailedResultReturnedByThePort() {
        EmailSenderPort port = message ->
                new EmailSendResult(EmailDeliveryStatus.FAILED, "RESEND", null, Instant.now(), "timeout");
        SendEmailApplicationService service = new SendEmailApplicationService(port, catalog, renderer);

        EmailSendResult result = service.execute(new SendEmailCommand(
                new EmailAddress("user@example.com"), EmailTemplateCategory.WELCOME, Map.of()));

        assertThat(result.status()).isEqualTo(EmailDeliveryStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo("timeout");
    }
}
