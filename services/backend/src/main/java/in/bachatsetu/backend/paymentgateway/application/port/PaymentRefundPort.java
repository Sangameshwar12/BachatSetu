package in.bachatsetu.backend.paymentgateway.application.port;

import in.bachatsetu.backend.paymentgateway.application.query.RefundResult;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;

/** Initiates a refund with the provider for a previously created order. One implementation per {@link GatewayType}. */
public interface PaymentRefundPort {

    GatewayType supportedProvider();

    RefundResult initiateRefund(AggregateId paymentId, String providerOrderId, Money amount);
}
