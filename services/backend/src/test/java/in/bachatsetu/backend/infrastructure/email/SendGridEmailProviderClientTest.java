package in.bachatsetu.backend.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import in.bachatsetu.backend.infrastructure.email.config.EmailProviderProperties;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

/** No live network call is made in this test — every HTTP exchange is stubbed by {@link MockRestServiceServer}. */
class SendGridEmailProviderClientTest {

    private static final EmailProviderMessage MESSAGE = new EmailProviderMessage(
            "user@example.com", "noreply@bachatsetu.in", "support@bachatsetu.in",
            "Welcome", "<p>hi</p>", "hi");
    private static final EmailProviderProperties.SendGrid CONFIG =
            new EmailProviderProperties.SendGrid("SG.test-api-key");

    @Test
    void sendsTheMessageAndReadsTheMessageIdFromTheResponseHeader() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ResponseCreator accepted = request -> {
            org.springframework.mock.http.client.MockClientHttpResponse response =
                    new org.springframework.mock.http.client.MockClientHttpResponse(new byte[0], HttpStatus.ACCEPTED);
            response.getHeaders().add("X-Message-Id", "sendgrid-msg-1");
            return response;
        };
        server.expect(requestTo("https://api.sendgrid.com/v3/mail/send"))
                .andExpect(header("Authorization", "Bearer SG.test-api-key"))
                .andExpect(jsonPath("$.subject").value("Welcome"))
                .andRespond(accepted);

        SendGridEmailProviderClient client = new SendGridEmailProviderClient(builder.build(), CONFIG);
        EmailProviderSendResult result = client.send(MESSAGE);

        assertThat(result.providerName()).isEqualTo("SENDGRID");
        assertThat(result.providerMessageId()).isEqualTo("sendgrid-msg-1");
        server.verify();
    }

    @Test
    void treatsALogicalRejectionAsNonRetryable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.sendgrid.com/v3/mail/send"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        SendGridEmailProviderClient client = new SendGridEmailProviderClient(builder.build(), CONFIG);

        assertThatThrownBy(() -> client.send(MESSAGE))
                .isInstanceOfSatisfying(EmailProviderException.class, exception ->
                        assertThat(exception.retryable()).isFalse());
    }

    @Test
    void treatsAServiceUnavailableResponseAsRetryable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.sendgrid.com/v3/mail/send"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        SendGridEmailProviderClient client = new SendGridEmailProviderClient(builder.build(), CONFIG);

        assertThatThrownBy(() -> client.send(MESSAGE))
                .isInstanceOfSatisfying(EmailProviderException.class, exception ->
                        assertThat(exception.retryable()).isTrue());
    }

    @Test
    void treatsANetworkFailureAsRetryable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.sendgrid.com/v3/mail/send"))
                .andRespond(request -> {
                    throw new IOException("simulated network failure");
                });

        SendGridEmailProviderClient client = new SendGridEmailProviderClient(builder.build(), CONFIG);

        assertThatThrownBy(() -> client.send(MESSAGE))
                .isInstanceOfSatisfying(EmailProviderException.class, exception -> {
                    assertThat(exception.retryable()).isTrue();
                    assertThat(exception.httpStatus()).isEqualTo(-1);
                });
    }
}
