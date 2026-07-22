package in.bachatsetu.backend.notification.interfaces.rest.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.email.application.port.EmailSenderPort;
import in.bachatsetu.backend.email.domain.model.EmailDeliveryStatus;
import in.bachatsetu.backend.email.domain.model.EmailMessage;
import in.bachatsetu.backend.email.domain.model.EmailSendResult;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RealEmailSenderAdapterTest {

    @Test
    void delegatesToTheSharedEmailSenderPortAndReturnsItsProviderMessageId() {
        EmailSenderPort emailSenderPort = mock(EmailSenderPort.class);
        when(emailSenderPort.send(any())).thenReturn(new EmailSendResult(
                EmailDeliveryStatus.SENT, "RESEND", "resend-message-id", Instant.now(), null));
        RealEmailSenderAdapter adapter = new RealEmailSenderAdapter(emailSenderPort);

        String providerMessageId = adapter.send(recipient(), content());

        assertThat(providerMessageId).isEqualTo("resend-message-id");
    }

    @Test
    void tagsTheDispatchedMessageAsAGeneralNotification() {
        EmailSenderPort emailSenderPort = mock(EmailSenderPort.class);
        when(emailSenderPort.send(any())).thenReturn(new EmailSendResult(
                EmailDeliveryStatus.SENT, "RESEND", "resend-message-id", Instant.now(), null));
        RealEmailSenderAdapter adapter = new RealEmailSenderAdapter(emailSenderPort);

        adapter.send(recipient(), content());

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSenderPort).send(captor.capture());
        assertThat(captor.getValue().category()).isEqualTo(EmailTemplateCategory.GENERAL_NOTIFICATION);
        assertThat(captor.getValue().to().value()).isEqualTo("member@example.com");
        assertThat(captor.getValue().content().subject()).isEqualTo("Account verification");
        assertThat(captor.getValue().content().textBody()).isEqualTo("Please verify your account.");
        assertThat(captor.getValue().content().htmlBody()).isEqualTo("<p>Please verify your account.</p>");
    }

    @Test
    void fallsBackToADefaultSubjectWhenTheNotificationHasNone() {
        EmailSenderPort emailSenderPort = mock(EmailSenderPort.class);
        when(emailSenderPort.send(any())).thenReturn(new EmailSendResult(
                EmailDeliveryStatus.SENT, "RESEND", "resend-message-id", Instant.now(), null));
        RealEmailSenderAdapter adapter = new RealEmailSenderAdapter(emailSenderPort);

        adapter.send(recipient(), new NotificationContent(null, "Please verify your account."));

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSenderPort).send(captor.capture());
        assertThat(captor.getValue().content().subject()).isEqualTo("BachatSetu notification");
    }

    @Test
    void throwsWhenTheUnderlyingSendFails() {
        EmailSenderPort emailSenderPort = mock(EmailSenderPort.class);
        when(emailSenderPort.send(any())).thenReturn(new EmailSendResult(
                EmailDeliveryStatus.FAILED, "RESEND", null, Instant.now(), "provider rejected the request"));
        RealEmailSenderAdapter adapter = new RealEmailSenderAdapter(emailSenderPort);

        assertThatThrownBy(() -> adapter.send(recipient(), content()))
                .isInstanceOf(EmailNotificationDeliveryException.class)
                .hasMessage("provider rejected the request");
    }

    @Test
    void rejectsNullConstructorArgument() {
        assertThatThrownBy(() -> new RealEmailSenderAdapter(null)).isInstanceOf(NullPointerException.class);
    }

    private NotificationRecipient recipient() {
        return new NotificationRecipient(AggregateId.newId(), "member@example.com");
    }

    private NotificationContent content() {
        return new NotificationContent("Account verification", "Please verify your account.");
    }
}
