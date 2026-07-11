package in.bachatsetu.backend.infrastructure.email;

import in.bachatsetu.backend.infrastructure.email.config.EmailProviderProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Calls the SendGrid v3 Mail Send API ({@code POST /v3/mail/send}), authenticated with a bearer
 * API key. SendGrid answers a successful send with an empty {@code 202 Accepted} body and the
 * provider message id in the {@code X-Message-Id} response header rather than a JSON body — the
 * one genuine shape difference from every other provider client in this package.
 */
public final class SendGridEmailProviderClient implements EmailProviderClient {

    private static final String ENDPOINT = "https://api.sendgrid.com/v3/mail/send";
    private static final String MESSAGE_ID_HEADER = "X-Message-Id";
    private static final int NO_HTTP_STATUS = -1;

    private final RestClient restClient;
    private final EmailProviderProperties.SendGrid config;

    public SendGridEmailProviderClient(RestClient restClient, EmailProviderProperties.SendGrid config) {
        this.restClient = Objects.requireNonNull(restClient, "rest client must not be null");
        this.config = Objects.requireNonNull(config, "SendGrid configuration must not be null");
    }

    @Override
    public EmailProviderSendResult send(EmailProviderMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(ENDPOINT)
                    .headers(headers -> headers.setBearerAuth(config.apiKey()))
                    .body(requestBody(message))
                    .retrieve()
                    .toBodilessEntity();
            HttpHeaders headers = response.getHeaders();
            String messageId = headers.getFirst(MESSAGE_ID_HEADER);
            if (messageId == null || messageId.isBlank()) {
                throw new EmailProviderException(
                        "SendGrid response did not contain " + MESSAGE_ID_HEADER, false, NO_HTTP_STATUS);
            }
            return new EmailProviderSendResult("SENDGRID", messageId, Instant.now());
        } catch (RestClientResponseException exception) {
            throw mapHttpFailure(exception);
        } catch (ResourceAccessException exception) {
            throw new EmailProviderException("SendGrid request failed: network error", true, NO_HTTP_STATUS, exception);
        }
    }

    private Map<String, Object> requestBody(EmailProviderMessage message) {
        return Map.of(
                "personalizations", List.of(Map.of("to", List.of(Map.of("email", message.to())))),
                "from", Map.of("email", message.from()),
                "reply_to", Map.of("email", message.replyTo()),
                "subject", message.subject(),
                "content", List.of(
                        Map.of("type", "text/plain", "value", message.textBody()),
                        Map.of("type", "text/html", "value", message.htmlBody())));
    }

    private EmailProviderException mapHttpFailure(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        boolean retryable = status == 502 || status == 503 || status == 504;
        return new EmailProviderException("SendGrid request failed with HTTP " + status, retryable, status, exception);
    }
}
