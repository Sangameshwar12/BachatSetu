package in.bachatsetu.backend.infrastructure.email;

import java.time.Instant;
import java.util.Objects;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

/**
 * Calls AWS SES's {@code SendEmail} API through the official synchronous SDK client — the one
 * provider in this package that talks to its provider via an SDK rather than raw {@code
 * RestClient} calls, since hand-rolling AWS's SigV4 request signing would be neither correct nor
 * production-ready. {@link SesClient} itself is the "provider SDK" the sprint's architecture
 * diagram keeps out of the application layer; this class is its only caller.
 */
public final class AwsSesEmailProviderClient implements EmailProviderClient {

    private static final int NO_HTTP_STATUS = -1;
    private static final String CHARSET = "UTF-8";

    private final SesClient sesClient;

    public AwsSesEmailProviderClient(SesClient sesClient) {
        this.sesClient = Objects.requireNonNull(sesClient, "SES client must not be null");
    }

    @Override
    public EmailProviderSendResult send(EmailProviderMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(message.from())
                    .destination(Destination.builder().toAddresses(message.to()).build())
                    .replyToAddresses(message.replyTo())
                    .message(Message.builder()
                            .subject(Content.builder().charset(CHARSET).data(message.subject()).build())
                            .body(Body.builder()
                                    .html(Content.builder().charset(CHARSET).data(message.htmlBody()).build())
                                    .text(Content.builder().charset(CHARSET).data(message.textBody()).build())
                                    .build())
                            .build())
                    .build();
            SendEmailResponse response = sesClient.sendEmail(request);
            return new EmailProviderSendResult("AWS_SES", response.messageId(), Instant.now());
        } catch (AwsServiceException exception) {
            throw mapServiceFailure(exception);
        } catch (SdkClientException exception) {
            throw new EmailProviderException(
                    "AWS SES request failed: network error", true, NO_HTTP_STATUS, exception);
        }
    }

    private EmailProviderException mapServiceFailure(AwsServiceException exception) {
        int status = exception.statusCode();
        boolean retryable = status == 429 || status == 502 || status == 503 || status == 504;
        return new EmailProviderException("AWS SES request failed with HTTP " + status, retryable, status, exception);
    }
}
