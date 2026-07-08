package in.bachatsetu.backend.paymentgateway.application.command;

import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import java.util.Objects;
import java.util.Set;

/**
 * Requests processing of one inbound webhook call. {@code rawPayload} is the exact request body the
 * signature was computed over; {@code providerOrderId}/{@code status}/{@code providerReferenceId} are
 * already parsed out of it by the REST-layer mapper before this command is built, since translating a
 * webhook body into typed fields is I/O-boundary translation, not business logic.
 *
 * <p>{@code status} is this module's own normalized contract — exactly {@code "SUCCESS"} or {@code
 * "FAILED"} — rather than each provider's real, differing status vocabulary; see
 * {@code docs/application/payment-gateway.md} for why.
 */
public record ProcessWebhookCommand(
        GatewayType provider,
        String rawPayload,
        String signatureHeader,
        String providerOrderId,
        String status,
        String providerReferenceId) {

    private static final Set<String> ALLOWED_STATUSES = Set.of("SUCCESS", "FAILED");

    public ProcessWebhookCommand {
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(rawPayload, "raw payload must not be null");
        Objects.requireNonNull(signatureHeader, "signature header must not be null");
        Objects.requireNonNull(providerOrderId, "provider order id must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("status must be one of " + ALLOWED_STATUSES);
        }
        Objects.requireNonNull(providerReferenceId, "provider reference id must not be null");
    }

    public boolean successful() {
        return "SUCCESS".equals(status);
    }
}
