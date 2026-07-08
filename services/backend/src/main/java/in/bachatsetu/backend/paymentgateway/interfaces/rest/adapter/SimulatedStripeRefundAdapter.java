package in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter;

import in.bachatsetu.backend.paymentgateway.application.port.PaymentRefundPort;
import in.bachatsetu.backend.paymentgateway.application.query.RefundResult;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simulates the Stripe Refunds API. No SDK or network call is used; see {@code SimulatedRazorpayGatewayAdapter}. */
public final class SimulatedStripeRefundAdapter implements PaymentRefundPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedStripeRefundAdapter.class);

    @Override
    public GatewayType supportedProvider() {
        return GatewayType.STRIPE;
    }

    @Override
    public RefundResult initiateRefund(AggregateId paymentId, String providerOrderId, Money amount) {
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(providerOrderId, "provider order id must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        String refundId = "re_" + UUID.randomUUID().toString().replace("-", "");
        LOGGER.info("Simulated Stripe refund {} initiated for payment intent {}", refundId, providerOrderId);
        return new RefundResult(paymentId.value(), GatewayType.STRIPE, refundId, true);
    }
}
