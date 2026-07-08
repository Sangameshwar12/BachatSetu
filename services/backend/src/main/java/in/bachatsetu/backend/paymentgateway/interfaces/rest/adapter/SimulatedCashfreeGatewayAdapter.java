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
 * Simulates the Cashfree Orders API. **No Cashfree SDK or network call is used**; see
 * {@link SimulatedRazorpayGatewayAdapter}'s Javadoc for why this and its sibling adapters are placeholders.
 */
public final class SimulatedCashfreeGatewayAdapter implements PaymentGatewayPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedCashfreeGatewayAdapter.class);

    @Override
    public GatewayType supportedProvider() {
        return GatewayType.CASHFREE;
    }

    @Override
    public PaymentOrderResult createOrder(AggregateId paymentId, Money amount) {
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        String orderId = "cf_order_" + UUID.randomUUID().toString().replace("-", "");
        String paymentLink = "https://payments.cashfree.example/pay/" + orderId;
        LOGGER.info("Simulated Cashfree order {} created for payment {}", orderId, paymentId);
        return new PaymentOrderResult(paymentId.value(), GatewayType.CASHFREE, orderId, paymentLink);
    }

    @Override
    public PaymentStatusResult fetchStatus(AggregateId paymentId, String providerOrderId) {
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(providerOrderId, "provider order id must not be null");
        LOGGER.info("Simulated Cashfree status fetch for order {}", providerOrderId);
        return new PaymentStatusResult(
                paymentId.value(), GatewayType.CASHFREE, providerOrderId, "SUCCESS", true, false);
    }
}
