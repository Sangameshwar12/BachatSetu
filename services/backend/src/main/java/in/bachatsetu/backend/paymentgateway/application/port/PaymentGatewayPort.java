package in.bachatsetu.backend.paymentgateway.application.port;

import in.bachatsetu.backend.paymentgateway.application.query.PaymentOrderResult;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentStatusResult;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;

/**
 * Creates a provider-side order for a payment and fetches its current status. One implementation exists
 * per {@link GatewayType}; application code selects one only by provider, never by concrete class, and
 * operates only on {@link PaymentOrderResult}/{@link PaymentStatusResult} — never a provider SDK type.
 */
public interface PaymentGatewayPort {

    GatewayType supportedProvider();

    PaymentOrderResult createOrder(AggregateId paymentId, Money amount);

    PaymentStatusResult fetchStatus(AggregateId paymentId, String providerOrderId);
}
