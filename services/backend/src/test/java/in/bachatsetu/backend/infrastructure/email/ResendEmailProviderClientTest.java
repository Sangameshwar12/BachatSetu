package in.bachatsetu.backend.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import in.bachatsetu.backend.infrastructure.email.config.EmailProviderProperties;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** No live network call is made in this test — every HTTP exchange is stubbed by {@link MockRestServiceServer}. */
class ResendEmailProviderClientTest {

    private static final EmailProviderMessage MESSAGE = new EmailProviderMessage(
            "user@example.com", "noreply@bachatsetu.in", "support@bachatsetu.in",
            "Welcome", "<p>hi</p>", "hi");
    private static final EmailProviderProperties.Resend CONFIG =
            new EmailProviderProperties.Resend("test-api-key");

    @Test
    void sendsTheMessageAndParsesTheMessageId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(jsonPath("$.from").value("noreply@bachatsetu.in"))
                .andExpect(jsonPath("$.subject").value("Welcome"))
                .andRespond(withSuccess("{\"id\":\"resend-msg-1\"}", MediaType.APPLICATION_JSON));

        ResendEmailProviderClient client = new ResendEmailProviderClient(builder.build(), CONFIG);
        EmailProviderSendResult result = client.send(MESSAGE);

        assertThat(result.providerName()).isEqualTo("RESEND");
        assertThat(result.providerMessageId()).isEqualTo("resend-msg-1");
        server.verify();
    }

    @Test
    void treatsALogicalRejectionAsNonRetryable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"message\":\"invalid api key\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        ResendEmailProviderClient client = new ResendEmailProviderClient(builder.build(), CONFIG);

        assertThatThrownBy(() -> client.send(MESSAGE))
                .isInstanceOfSatisfying(EmailProviderException.class, exception ->
                        assertThat(exception.retryable()).isFalse());
    }

    @Test
    void treatsABadGatewayResponseAsRetryable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        ResendEmailProviderClient client = new ResendEmailProviderClient(builder.build(), CONFIG);

        assertThatThrownBy(() -> client.send(MESSAGE))
                .isInstanceOfSatisfying(EmailProviderException.class, exception ->
                        assertThat(exception.retryable()).isTrue());
    }

    @Test
    void treatsANetworkFailureAsRetryable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(request -> {
                    throw new IOException("simulated network failure");
                });

        ResendEmailProviderClient client = new ResendEmailProviderClient(builder.build(), CONFIG);

        assertThatThrownBy(() -> client.send(MESSAGE))
                .isInstanceOfSatisfying(EmailProviderException.class, exception -> {
                    assertThat(exception.retryable()).isTrue();
                    assertThat(exception.httpStatus()).isEqualTo(-1);
                });
    }
}
