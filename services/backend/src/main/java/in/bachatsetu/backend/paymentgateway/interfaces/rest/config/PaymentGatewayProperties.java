package in.bachatsetu.backend.paymentgateway.interfaces.rest.config;

import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly typed Payment Gateway configuration. Every secret defaults to an empty string via
 * {@code ${ENV_VAR:}} placeholders in {@code application.yml} — never a hardcoded value — so a blank secret
 * simply means signature verification always fails safely (see {@code HmacSha256Signer}) until a real
 * secret is supplied through the environment.
 */
@ConfigurationProperties(prefix = "bachatsetu.payment.gateway")
public record PaymentGatewayProperties(
        boolean enabled, GatewayType defaultProvider, Razorpay razorpay, Stripe stripe, Cashfree cashfree) {

    public PaymentGatewayProperties {
        Objects.requireNonNull(defaultProvider, "default provider must not be null");
        Objects.requireNonNull(razorpay, "razorpay configuration must not be null");
        Objects.requireNonNull(stripe, "stripe configuration must not be null");
        Objects.requireNonNull(cashfree, "cashfree configuration must not be null");
    }

    public record Razorpay(String keyId, String secret, String webhookSecret) {
        public Razorpay {
            keyId = Objects.requireNonNullElse(keyId, "");
            secret = Objects.requireNonNullElse(secret, "");
            webhookSecret = Objects.requireNonNullElse(webhookSecret, "");
        }
    }

    public record Stripe(String apiKey, String webhookSecret) {
        public Stripe {
            apiKey = Objects.requireNonNullElse(apiKey, "");
            webhookSecret = Objects.requireNonNullElse(webhookSecret, "");
        }
    }

    public record Cashfree(String clientId, String clientSecret, String webhookSecret) {
        public Cashfree {
            clientId = Objects.requireNonNullElse(clientId, "");
            clientSecret = Objects.requireNonNullElse(clientSecret, "");
            webhookSecret = Objects.requireNonNullElse(webhookSecret, "");
        }
    }
}
