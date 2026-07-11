package in.bachatsetu.backend.infrastructure.email;

import in.bachatsetu.backend.infrastructure.email.config.EmailProviderProperties;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Calls the Resend API ({@code POST /emails}), authenticated with a bearer API key. Resend
 * reports failure through a proper non-2xx HTTP status with a JSON error body, so no separate
 * logical-failure-on-200 handling is needed, unlike MSG91/Fast2SMS's SMS equivalents.
 */
public final class ResendEmailProviderClient implements EmailProviderClient {

    private static final String ENDPOINT = "https://api.resend.com/emails";
    private static final int NO_HTTP_STATUS = -1;
    private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;
    private final EmailProviderProperties.Resend config;

    public ResendEmailProviderClient(RestClient restClient, EmailProviderProperties.Resend config) {
        this.restClient = Objects.requireNonNull(restClient, "rest client must not be null");
        this.config = Objects.requireNonNull(config, "Resend configuration must not be null");
    }

    @Override
    public EmailProviderSendResult send(EmailProviderMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        try {
            Map<String, Object> body = restClient.post()
                    .uri(ENDPOINT)
                    .headers(headers -> headers.setBearerAuth(config.apiKey()))
                    .body(Map.of(
                            "from", message.from(),
                            "to", java.util.List.of(message.to()),
                            "reply_to", message.replyTo(),
                            "subject", message.subject(),
                            "html", message.htmlBody(),
                            "text", message.textBody()))
                    .retrieve()
                    .body(RESPONSE_TYPE);
            Object id = body == null ? null : body.get("id");
            if (id == null) {
                throw new EmailProviderException("Resend response did not contain a message id", false, NO_HTTP_STATUS);
            }
            return new EmailProviderSendResult("RESEND", String.valueOf(id), Instant.now());
        } catch (RestClientResponseException exception) {
            throw mapHttpFailure(exception);
        } catch (ResourceAccessException exception) {
            throw new EmailProviderException("Resend request failed: network error", true, NO_HTTP_STATUS, exception);
        }
    }

    private EmailProviderException mapHttpFailure(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        boolean retryable = status == 502 || status == 503 || status == 504;
        return new EmailProviderException("Resend request failed with HTTP " + status, retryable, status, exception);
    }
}
