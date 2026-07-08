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

/** Simulates the Razorpay Refunds API. No SDK or network call is used; see {@code SimulatedRazorpayGatewayAdapter}. */
public final class SimulatedRazorpayRefundAdapter implements PaymentRefundPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedRazorpayRefundAdapter.class);

    @Override
    public GatewayType supportedProvider() {
        return GatewayType.RAZORPAY;
    }

    @Override
    public RefundResult initiateRefund(AggregateId paymentId, String providerOrderId, Money amount) {
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(providerOrderId, "provider order id must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        String refundId = "rfnd_" + UUID.randomUUID().toString().replace("-", "");
        LOGGER.info("Simulated Razorpay refund {} initiated for order {}", refundId, providerOrderId);
        return new RefundResult(paymentId.value(), GatewayType.RAZORPAY, refundId, true);
    }
}
