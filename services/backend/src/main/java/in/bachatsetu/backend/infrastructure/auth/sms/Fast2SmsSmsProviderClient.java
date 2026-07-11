package in.bachatsetu.backend.infrastructure.auth.sms;

import in.bachatsetu.backend.infrastructure.auth.config.SmsProviderProperties;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Calls Fast2SMS's OTP route ({@code POST /dev/bulkV2}, {@code route=otp}), which — like MSG91 —
 * frequently answers with HTTP 200 even for a logically rejected request, signalling failure
 * through {@code "return": false} in the JSON body rather than an HTTP error status.
 */
public final class Fast2SmsSmsProviderClient implements SmsProviderClient {

    private static final String ENDPOINT = "https://www.fast2sms.com/dev/bulkV2";
    private static final int NO_HTTP_STATUS = -1;
    private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;
    private final SmsProviderProperties.Fast2Sms config;

    public Fast2SmsSmsProviderClient(RestClient restClient, SmsProviderProperties.Fast2Sms config) {
        this.restClient = Objects.requireNonNull(restClient, "rest client must not be null");
        this.config = Objects.requireNonNull(config, "Fast2SMS configuration must not be null");
    }

    @Override
    public SmsSendResult send(SmsMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        String nationalNumber = message.toE164().substring(3);
        try {
            Map<String, Object> body = restClient.post()
                    .uri(ENDPOINT)
                    .header("authorization", config.apiKey())
                    .body(Map.of(
                            "route", "otp",
                            "variables_values", message.otpCode(),
                            "numbers", nationalNumber))
                    .retrieve()
                    .body(RESPONSE_TYPE);
            return handleResponse(body);
        } catch (RestClientResponseException exception) {
            throw mapHttpFailure(exception);
        } catch (ResourceAccessException exception) {
            throw new SmsProviderException(
                    "Fast2SMS request failed: network error", true, NO_HTTP_STATUS, exception);
        }
    }

    private SmsSendResult handleResponse(Map<String, Object> body) {
        Object success = body == null ? null : body.get("return");
        Object requestId = body == null ? null : body.get("request_id");
        if (!Boolean.TRUE.equals(success)) {
            throw new SmsProviderException("Fast2SMS rejected the OTP request", false, NO_HTTP_STATUS);
        }
        return new SmsSendResult("FAST2SMS", String.valueOf(requestId), Instant.now());
    }

    private SmsProviderException mapHttpFailure(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        boolean retryable = status == 502 || status == 503 || status == 504;
        return new SmsProviderException(
                "Fast2SMS request failed with HTTP " + status, retryable, status, exception);
    }
}
