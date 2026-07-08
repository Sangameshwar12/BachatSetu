package in.bachatsetu.backend.paymentgateway.application.service;

import in.bachatsetu.backend.paymentgateway.application.exception.UnsupportedGatewayException;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentGatewayPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentRefundPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentWebhookVerifierPort;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import java.util.List;

/**
 * Selects the one adapter matching a requested {@link GatewayType} out of every adapter Spring wires for a
 * given port — the mechanism behind "provider abstraction" and "provider switching": application code
 * never names a concrete Razorpay/Stripe/Cashfree class, only a {@link GatewayType}.
 */
final class GatewayPortResolver {

    private GatewayPortResolver() {
    }

    static PaymentGatewayPort resolveGateway(List<PaymentGatewayPort> gateways, GatewayType provider) {
        return gateways.stream()
                .filter(gateway -> gateway.supportedProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new UnsupportedGatewayException("no payment gateway registered for " + provider));
    }

    static PaymentRefundPort resolveRefundPort(List<PaymentRefundPort> refundPorts, GatewayType provider) {
        return refundPorts.stream()
                .filter(refundPort -> refundPort.supportedProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new UnsupportedGatewayException("no refund gateway registered for " + provider));
    }

    static PaymentWebhookVerifierPort resolveVerifier(List<PaymentWebhookVerifierPort> verifiers, GatewayType provider) {
        return verifiers.stream()
                .filter(verifier -> verifier.supportedProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new UnsupportedGatewayException("no webhook verifier registered for " + provider));
    }
}
