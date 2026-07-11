package in.bachatsetu.backend.infrastructure.auth.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import in.bachatsetu.backend.infrastructure.auth.config.SmsProviderProperties;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** No live network call is made in this test — every HTTP exchange is stubbed by {@link MockRestServiceServer}. */
class Fast2SmsSmsProviderClientTest {

    private static final SmsMessage MESSAGE = new SmsMessage(
            "+919876543210", "123456", "Your BachatSetu OTP is 123456.");
    private static final SmsProviderProperties.Fast2Sms CONFIG =
            new SmsProviderProperties.Fast2Sms("test-api-key");

    @Test
    void sendsTheOtpWithoutTheCountryCodeAndParsesTheRequestId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://www.fast2sms.com/dev/bulkV2"))
                .andExpect(header("authorization", "test-api-key"))
                .andExpect(jsonPath("$.numbers").value("9876543210"))
                .andExpect(jsonPath("$.variables_values").value("123456"))
                .andExpect(jsonPath("$.route").value("otp"))
                .andRespond(withSuccess(
                        "{\"return\":true,\"request_id\":\"req-456\"}", MediaType.APPLICATION_JSON));

        Fast2SmsSmsProviderClient client = new Fast2SmsSmsProviderClient(builder.build(), CONFIG);
        SmsSendResult result = client.send(MESSAGE);

        assertThat(result.providerName()).isEqualTo("FAST2SMS");
        assertThat(result.providerMessageId()).isEqualTo("req-456");
        server.verify();
    }

    @Test
    void treatsAHttp200LogicalRejectionAsNonRetryable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://www.fast2sms.com/dev/bulkV2"))
                .andRespond(withSuccess("{\"return\":false,\"message\":\"invalid api key\"}", MediaType.APPLICATION_JSON));

        Fast2SmsSmsProviderClient client = new Fast2SmsSmsProviderClient(builder.build(), CONFIG);

        assertThatThrownBy(() -> client.send(MESSAGE))
                .isInstanceOfSatisfying(SmsProviderException.class, exception ->
                        assertThat(exception.retryable()).isFalse());
    }

    @Test
    void treatsABadGatewayResponseAsRetryable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://www.fast2sms.com/dev/bulkV2"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        Fast2SmsSmsProviderClient client = new Fast2SmsSmsProviderClient(builder.build(), CONFIG);

        assertThatThrownBy(() -> client.send(MESSAGE))
                .isInstanceOfSatisfying(SmsProviderException.class, exception ->
                        assertThat(exception.retryable()).isTrue());
    }

    @Test
    void treatsANetworkFailureAsRetryable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://www.fast2sms.com/dev/bulkV2"))
                .andRespond(request -> {
                    throw new IOException("simulated network failure");
                });

        Fast2SmsSmsProviderClient client = new Fast2SmsSmsProviderClient(builder.build(), CONFIG);

        assertThatThrownBy(() -> client.send(MESSAGE))
                .isInstanceOfSatisfying(SmsProviderException.class, exception -> {
                    assertThat(exception.retryable()).isTrue();
                    assertThat(exception.httpStatus()).isEqualTo(-1);
                });
    }
}
