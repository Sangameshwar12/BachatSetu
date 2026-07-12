package in.bachatsetu.backend.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

/** No live AWS call is made in this test — {@link SesClient} is mocked. */
class AwsSesEmailProviderClientTest {

    private static final EmailProviderMessage MESSAGE = new EmailProviderMessage(
            "user@example.com", "noreply@bachatsetu.in", "support@bachatsetu.in",
            "Welcome", "<p>hi</p>", "hi");

    @Test
    void sendsTheMessageAndReturnsTheSesMessageId() {
        try (SesClient sesClient = mock(SesClient.class)) {
            when(sesClient.sendEmail(any(SendEmailRequest.class)))
                    .thenReturn(SendEmailResponse.builder().messageId("ses-msg-1").build());

            AwsSesEmailProviderClient client = new AwsSesEmailProviderClient(sesClient);
            EmailProviderSendResult result = client.send(MESSAGE);

            assertThat(result.providerName()).isEqualTo("AWS_SES");
            assertThat(result.providerMessageId()).isEqualTo("ses-msg-1");
        }
    }

    @Test
    void treatsALogicalRejectionAsNonRetryable() {
        try (SesClient sesClient = mock(SesClient.class)) {
            when(sesClient.sendEmail(any(SendEmailRequest.class)))
                    .thenThrow(AwsServiceException.builder().statusCode(400).message("invalid recipient").build());

            AwsSesEmailProviderClient client = new AwsSesEmailProviderClient(sesClient);

            assertThatThrownBy(() -> client.send(MESSAGE))
                    .isInstanceOfSatisfying(EmailProviderException.class, exception ->
                            assertThat(exception.retryable()).isFalse());
        }
    }

    @Test
    void treatsAServiceUnavailableResponseAsRetryable() {
        try (SesClient sesClient = mock(SesClient.class)) {
            when(sesClient.sendEmail(any(SendEmailRequest.class)))
                    .thenThrow(AwsServiceException.builder().statusCode(503).message("throttled").build());

            AwsSesEmailProviderClient client = new AwsSesEmailProviderClient(sesClient);

            assertThatThrownBy(() -> client.send(MESSAGE))
                    .isInstanceOfSatisfying(EmailProviderException.class, exception ->
                            assertThat(exception.retryable()).isTrue());
        }
    }

    @Test
    void treatsANetworkFailureAsRetryable() {
        try (SesClient sesClient = mock(SesClient.class)) {
            when(sesClient.sendEmail(any(SendEmailRequest.class)))
                    .thenThrow(SdkClientException.create("simulated network failure"));

            AwsSesEmailProviderClient client = new AwsSesEmailProviderClient(sesClient);

            assertThatThrownBy(() -> client.send(MESSAGE))
                    .isInstanceOfSatisfying(EmailProviderException.class, exception -> {
                        assertThat(exception.retryable()).isTrue();
                        assertThat(exception.httpStatus()).isEqualTo(-1);
                    });
        }
    }
}
