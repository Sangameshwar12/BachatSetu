package in.bachatsetu.backend.infrastructure.auth.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import in.bachatsetu.backend.infrastructure.auth.config.SmsProviderProperties;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** No live network call is made in this test — every HTTP exchange is stubbed by {@link MockRestServiceServer}. */
class Msg91SmsProviderClientTest {

    private static final SmsMessage MESSAGE = new SmsMessage(
            "+919876543210", "123456", "Your BachatSetu OTP is 123456.");
    private static final SmsProviderProperties.Msg91 CONFIG =
            new SmsProviderProperties.Msg91("test-auth-key", "test-template", "BACHAT");

    @Test
    void sendsTheOtpAndParsesTheProviderMessageIdOnSuccess() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://control.msg91.com/api/v5/otp"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("authkey", "test-auth-key"))
                .andExpect(jsonPath("$.mobile").value("919876543210"))
                .andExpect(jsonPath("$.otp").value("123456"))
                .andExpect(jsonPath("$.template_id").value("test-template"))
                .andRespond(withSuccess("{\"type\":\"success\",\"message\":\"req-123\"}", MediaType.APPLICATION_JSON));

        Msg91SmsProviderClient client = new Msg91SmsProviderClient(builder.build(), CONFIG);
        SmsSendResult result = client.send(MESSAGE);

        assertThat(result.providerName()).isEqualTo("MSG91");
        assertThat(result.providerMessageId()).isEqualTo("req-123");
        server.verify();
    }

    @Test
    void treatsAHttp200LogicalRejectionAsANonRetryableFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://control.msg91.com/api/v5/otp"))
                .andRespond(withSuccess("{\"type\":\"error\",\"message\":\"invalid mobile number\"}",
                        MediaType.APPLICATION_JSON));

        Msg91SmsProviderClient client = new Msg91SmsProviderClient(builder.build(), CONFIG);

        assertThatThrownBy(() -> client.send(MESSAGE))
                .isInstanceOfSatisfying(SmsProviderException.class, exception ->
                        assertThat(exception.retryable()).isFalse());
    }

    @Test
    void treatsAServiceUnavailableResponseAsRetryable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://control.msg91.com/api/v5/otp"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        Msg91SmsProviderClient client = new Msg91SmsProviderClient(builder.build(), CONFIG);

        assertThatThrownBy(() -> client.send(MESSAGE))
                .isInstanceOfSatisfying(SmsProviderException.class, exception -> {
                    assertThat(exception.retryable()).isTrue();
                    assertThat(exception.httpStatus()).isEqualTo(503);
                });
    }

    @Test
    void treatsANetworkFailureAsRetryable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://control.msg91.com/api/v5/otp"))
                .andRespond(request -> {
                    throw new IOException("simulated network failure");
                });

        Msg91SmsProviderClient client = new Msg91SmsProviderClient(builder.build(), CONFIG);

        assertThatThrownBy(() -> client.send(MESSAGE))
                .isInstanceOfSatisfying(SmsProviderException.class, exception -> {
                    assertThat(exception.retryable()).isTrue();
                    assertThat(exception.httpStatus()).isEqualTo(-1);
                });
    }

    @Test
    void treatsAPlainInternalServerErrorAsNonRetryableUnlikeGatewayErrors() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://control.msg91.com/api/v5/otp"))
                .andRespond(withServerError().body("").contentType(MediaType.APPLICATION_JSON));

        Msg91SmsProviderClient client = new Msg91SmsProviderClient(builder.build(), CONFIG);

        assertThatThrownBy(() -> client.send(MESSAGE))
                .isInstanceOfSatisfying(SmsProviderException.class, exception ->
                        assertThat(exception.retryable()).isFalse());
    }
}
