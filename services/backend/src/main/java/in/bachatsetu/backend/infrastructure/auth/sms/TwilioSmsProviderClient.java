package in.bachatsetu.backend.infrastructure.auth.sms;

import in.bachatsetu.backend.infrastructure.auth.config.SmsProviderProperties;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Calls the Twilio Programmable Messaging API
 * ({@code POST /2010-04-01/Accounts/{AccountSid}/Messages.json}), authenticated with HTTP Basic
 * auth (Account SID as username, Auth Token as password) and a form-urlencoded body — unlike
 * MSG91/Fast2SMS, Twilio reports failure through a proper non-2xx HTTP status with a JSON error
 * body, so no separate logical-failure-on-200 handling is needed here.
 */
public final class TwilioSmsProviderClient implements SmsProviderClient {

    private static final String ENDPOINT_TEMPLATE =
            "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json";
    private static final int NO_HTTP_STATUS = -1;
    private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;
    private final SmsProviderProperties.Twilio config;

    public TwilioSmsProviderClient(RestClient restClient, SmsProviderProperties.Twilio config) {
        this.restClient = Objects.requireNonNull(restClient, "rest client must not be null");
        this.config = Objects.requireNonNull(config, "Twilio configuration must not be null");
    }

    @Override
    public SmsSendResult send(SmsMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", message.toE164());
        form.add("From", config.phoneNumber());
        form.add("Body", message.messageBody());
        try {
            Map<String, Object> body = restClient.post()
                    .uri(String.format(ENDPOINT_TEMPLATE, config.accountSid()))
                    .headers(headers -> headers.setBasicAuth(config.accountSid(), config.authToken()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(RESPONSE_TYPE);
            Object sid = body == null ? null : body.get("sid");
            return new SmsSendResult("TWILIO", String.valueOf(sid), Instant.now());
        } catch (RestClientResponseException exception) {
            throw mapHttpFailure(exception);
        } catch (ResourceAccessException exception) {
            throw new SmsProviderException(
                    "Twilio request failed: network error", true, NO_HTTP_STATUS, exception);
        }
    }

    private SmsProviderException mapHttpFailure(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        boolean retryable = status == 502 || status == 503 || status == 504;
        return new SmsProviderException("Twilio request failed with HTTP " + status, retryable, status, exception);
    }
}
