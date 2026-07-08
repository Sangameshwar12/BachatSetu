package in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Computes and compares HMAC-SHA256 webhook signatures — the mechanism all three supported providers use
 * in practice (Razorpay, Stripe, and Cashfree each sign their webhook body with HMAC-SHA256 and a
 * per-account secret, encoded as a hex or base64 string in a provider-specific header). This class needs no
 * provider SDK: it is standard JDK cryptography.
 */
final class HmacSha256Signer {

    private static final String ALGORITHM = "HmacSHA256";

    private HmacSha256Signer() {
    }

    /**
     * @return {@code false} (never throws) when {@code secret} or {@code signatureHeader} is blank, so a
     *         missing configuration value fails signature verification safely rather than raising an error.
     */
    static boolean matches(String payload, String secret, String signatureHeader) {
        Objects.requireNonNull(payload, "payload must not be null");
        if (secret == null || secret.isBlank() || signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        String expected = sign(payload, secret);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), signatureHeader.trim().getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("failed to compute HMAC-SHA256 signature", exception);
        }
    }
}
