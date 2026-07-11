package in.bachatsetu.backend.email.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class EmailSendResultTest {

    @Test
    void aSentResultRequiresAProviderMessageId() {
        assertThatThrownBy(() -> new EmailSendResult(
                EmailDeliveryStatus.SENT, "RESEND", null, Instant.now(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aFailedResultRequiresAFailureReason() {
        assertThatThrownBy(() -> new EmailSendResult(
                EmailDeliveryStatus.FAILED, "RESEND", null, Instant.now(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aSentResultWithAMessageIdIsValid() {
        EmailSendResult result = new EmailSendResult(
                EmailDeliveryStatus.SENT, "RESEND", "msg-1", Instant.now(), null);
        assertThat(result.status()).isEqualTo(EmailDeliveryStatus.SENT);
        assertThat(result.providerMessageId()).isEqualTo("msg-1");
    }

    @Test
    void aFailedResultWithAReasonIsValid() {
        EmailSendResult result = new EmailSendResult(
                EmailDeliveryStatus.FAILED, "RESEND", null, Instant.now(), "timeout");
        assertThat(result.status()).isEqualTo(EmailDeliveryStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo("timeout");
    }
}
