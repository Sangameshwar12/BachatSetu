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
 * Calls MSG91's OTP API ({@code POST /api/v5/otp}), which sends a template-rendered OTP SMS
 * given a template id, the destination mobile number, and the OTP value itself — MSG91 renders
 * the message text server-side from the template, so no message body is sent from here.
 *
 * <p>MSG91 is known to answer with HTTP 200 even for a logically rejected request (invalid
 * template, invalid mobile number, exhausted balance), signalling failure only through
 * {@code "type": "error"} in the JSON body — so a 200 response is not treated as success without
 * also checking that field.
 */
public final class Msg91SmsProviderClient implements SmsProviderClient {

    private static final String ENDPOINT = "https://control.msg91.com/api/v5/otp";
    private static final int NO_HTTP_STATUS = -1;
    private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;
    private final SmsProviderProperties.Msg91 config;

    public Msg91SmsProviderClient(RestClient restClient, SmsProviderProperties.Msg91 config) {
        this.restClient = Objects.requireNonNull(restClient, "rest client must not be null");
        this.config = Objects.requireNonNull(config, "MSG91 configuration must not be null");
    }

    @Override
    public SmsSendResult send(SmsMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        String nationalNumber = "91" + message.toE164().substring(3);
        try {
            Map<String, Object> body = restClient.post()
                    .uri(ENDPOINT)
                    .header("authkey", config.authKey())
                    .body(Map.of(
                            "template_id", config.templateId(),
                            "mobile", nationalNumber,
                            "otp", message.otpCode(),
                            "sender", config.senderId()))
                    .retrieve()
                    .body(RESPONSE_TYPE);
            return handleResponse(body);
        } catch (RestClientResponseException exception) {
            throw mapHttpFailure(exception);
        } catch (ResourceAccessException exception) {
            throw new SmsProviderException("MSG91 request failed: network error", true, NO_HTTP_STATUS, exception);
        }
    }

    private SmsSendResult handleResponse(Map<String, Object> body) {
        Object type = body == null ? null : body.get("type");
        Object requestId = body == null ? null : body.get("message");
        if (!"success".equals(type)) {
            throw new SmsProviderException("MSG91 rejected the OTP request", false, NO_HTTP_STATUS);
        }
        return new SmsSendResult("MSG91", String.valueOf(requestId), Instant.now());
    }

    private SmsProviderException mapHttpFailure(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        boolean retryable = status == 502 || status == 503 || status == 504;
        return new SmsProviderException("MSG91 request failed with HTTP " + status, retryable, status, exception);
    }
}
