package in.bachatsetu.backend.infrastructure.auth.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import in.bachatsetu.backend.infrastructure.auth.config.SmsProviderProperties;
import java.io.IOException;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** No live network call is made in this test — every HTTP exchange is stubbed by {@link MockRestServiceServer}. */
class TwilioSmsProviderClientTest {

    private static final SmsMessage MESSAGE = new SmsMessage(
            "+919876543210", "123456", "Your BachatSetu OTP is 123456.");
    private static final SmsProviderProperties.Twilio CONFIG =
            new SmsProviderProperties.Twilio("AC-test-sid", "test-auth-token", "+15005550006");

    @Test
    void sendsAFormEncodedRequestWithBasicAuthAndParsesTheMessageSid() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String expectedAuth = "Basic " + Base64.getEncoder()
                .encodeToString("AC-test-sid:test-auth-token".getBytes(StandardCharsets.UTF_8));
        server.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/AC-test-sid/Messages.json"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", expectedAuth))
                .andRespond(withSuccess("{\"sid\":\"SM123\",\"status\":\"queued\"}", MediaType.APPLICATION_JSON));

        TwilioSmsProviderClient client = new TwilioSmsProviderClient(builder.build(), CONFIG);
        SmsSendResult result = client.send(MESSAGE);

        assertThat(result.providerName()).isEqualTo("TWILIO");
        assertThat(result.providerMessageId()).isEqualTo("SM123");
        server.verify();
    }

    @Test
    void treatsAnUnauthorizedResponseAsNonRetryable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/AC-test-sid/Messages.json"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"code\":20003,\"message\":\"Authentication Error\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        TwilioSmsProviderClient client = new TwilioSmsProviderClient(builder.build(), CONFIG);

        assertThatThrownBy(() -> client.send(MESSAGE))
                .isInstanceOfSatisfying(SmsProviderException.class, exception -> {
                    assertThat(exception.retryable()).isFalse();
                    assertThat(exception.httpStatus()).isEqualTo(401);
                });
    }

    @Test
    void treatsAGatewayTimeoutAsRetryable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/AC-test-sid/Messages.json"))
                .andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));

        TwilioSmsProviderClient client = new TwilioSmsProviderClient(builder.build(), CONFIG);

        assertThatThrownBy(() -> client.send(MESSAGE))
                .isInstanceOfSatisfying(SmsProviderException.class, exception ->
                        assertThat(exception.retryable()).isTrue());
    }

    @Test
    void treatsANetworkFailureAsRetryable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/AC-test-sid/Messages.json"))
                .andRespond(request -> {
                    throw new IOException("simulated network failure");
                });

        TwilioSmsProviderClient client = new TwilioSmsProviderClient(builder.build(), CONFIG);

        assertThatThrownBy(() -> client.send(MESSAGE))
                .isInstanceOfSatisfying(SmsProviderException.class, exception -> {
                    assertThat(exception.retryable()).isTrue();
                    assertThat(exception.httpStatus()).isEqualTo(-1);
                });
    }
}
