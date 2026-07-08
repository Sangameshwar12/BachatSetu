package in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter;

import in.bachatsetu.backend.paymentgateway.application.port.PaymentGatewayPort;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentOrderResult;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentStatusResult;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simulates the Razorpay Orders API. **No Razorpay SDK or network call is used** — this generates a
 * deterministic-looking fake order id and payment link and logs what a real integration would do, exactly
 * as {@code LoggingEmailSenderAdapter}/etc. did for Notification's channels in Sprint 11.7. Real Razorpay
 * SDK integration (API keys, actual HTTP calls) is a future concern; this adapter exists so order
 * creation, status sync, and the rest of this module's orchestration are present, wired, and testable
 * ahead of that integration, without requiring live credentials in this environment.
 */
public final class SimulatedRazorpayGatewayAdapter implements PaymentGatewayPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedRazorpayGatewayAdapter.class);

    @Override
    public GatewayType supportedProvider() {
        return GatewayType.RAZORPAY;
    }

    @Override
    public PaymentOrderResult createOrder(AggregateId paymentId, Money amount) {
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        String orderId = "order_rzp_" + UUID.randomUUID().toString().replace("-", "");
        String paymentLink = "https://razorpay.example/pay/" + orderId;
        LOGGER.info("Simulated Razorpay order {} created for payment {}", orderId, paymentId);
        return new PaymentOrderResult(paymentId.value(), GatewayType.RAZORPAY, orderId, paymentLink);
    }

    @Override
    public PaymentStatusResult fetchStatus(AggregateId paymentId, String providerOrderId) {
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(providerOrderId, "provider order id must not be null");
        LOGGER.info("Simulated Razorpay status fetch for order {}", providerOrderId);
        return new PaymentStatusResult(
                paymentId.value(), GatewayType.RAZORPAY, providerOrderId, "captured", true, false);
    }
}
