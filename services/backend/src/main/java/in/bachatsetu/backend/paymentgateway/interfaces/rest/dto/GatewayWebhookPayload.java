package in.bachatsetu.backend.paymentgateway.interfaces.rest.dto;

/**
 * This module's own normalized webhook body contract — not any real provider's actual webhook JSON shape.
 * Razorpay, Stripe, and Cashfree each define their own, differing webhook payload formats; mapping each
 * provider's real format to this contract is future work once real SDKs are integrated (see
 * {@code docs/application/payment-gateway.md}). {@code status} must be exactly {@code "SUCCESS"} or
 * {@code "FAILED"}.
 */
public record GatewayWebhookPayload(String providerOrderId, String status, String providerReferenceId) {
}
