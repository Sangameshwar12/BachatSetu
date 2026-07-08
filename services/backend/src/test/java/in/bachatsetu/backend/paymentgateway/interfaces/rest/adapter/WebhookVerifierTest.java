package in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class WebhookVerifierTest {

    private static final String SECRET = "whsec_test_secret";
    private static final String PAYLOAD = "{\"providerOrderId\":\"order_1\",\"status\":\"SUCCESS\"}";

    @Test
    void razorpayVerifierAcceptsACorrectSignatureAndRejectsAWrongOne() {
        RazorpayWebhookVerifier verifier = new RazorpayWebhookVerifier(SECRET);
        String validSignature = hmacHex(PAYLOAD, SECRET);

        assertThat(verifier.supportedProvider()).isEqualTo(GatewayType.RAZORPAY);
        assertThat(verifier.verifySignature(PAYLOAD, validSignature)).isTrue();
        assertThat(verifier.verifySignature(PAYLOAD, "wrong-signature")).isFalse();
        assertThat(verifier.verifySignature("different payload", validSignature)).isFalse();
    }

    @Test
    void stripeVerifierAcceptsACorrectSignatureAndRejectsAWrongOne() {
        StripeWebhookVerifier verifier = new StripeWebhookVerifier(SECRET);
        String validSignature = hmacHex(PAYLOAD, SECRET);

        assertThat(verifier.supportedProvider()).isEqualTo(GatewayType.STRIPE);
        assertThat(verifier.verifySignature(PAYLOAD, validSignature)).isTrue();
        assertThat(verifier.verifySignature(PAYLOAD, "wrong-signature")).isFalse();
    }

    @Test
    void cashfreeVerifierAcceptsACorrectSignatureAndRejectsAWrongOne() {
        CashfreeWebhookVerifier verifier = new CashfreeWebhookVerifier(SECRET);
        String validSignature = hmacHex(PAYLOAD, SECRET);

        assertThat(verifier.supportedProvider()).isEqualTo(GatewayType.CASHFREE);
        assertThat(verifier.verifySignature(PAYLOAD, validSignature)).isTrue();
        assertThat(verifier.verifySignature(PAYLOAD, "wrong-signature")).isFalse();
    }

    @Test
    void failsSafelyWhenTheSecretIsBlank() {
        RazorpayWebhookVerifier verifier = new RazorpayWebhookVerifier("");

        assertThat(verifier.verifySignature(PAYLOAD, hmacHex(PAYLOAD, SECRET))).isFalse();
    }

    @Test
    void failsSafelyWhenTheSignatureHeaderIsMissing() {
        RazorpayWebhookVerifier verifier = new RazorpayWebhookVerifier(SECRET);

        assertThat(verifier.verifySignature(PAYLOAD, null)).isFalse();
        assertThat(verifier.verifySignature(PAYLOAD, "")).isFalse();
    }

    private static String hmacHex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
