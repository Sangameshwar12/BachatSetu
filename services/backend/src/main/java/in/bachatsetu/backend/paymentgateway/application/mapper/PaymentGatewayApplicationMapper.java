package in.bachatsetu.backend.paymentgateway.application.mapper;

import in.bachatsetu.backend.paymentgateway.application.query.PaymentOrderResult;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayOrder;
import java.util.Objects;

/** Converts the {@link GatewayOrder} aggregate to its application-layer read model. */
public final class PaymentGatewayApplicationMapper {

    public PaymentOrderResult toOrderResult(GatewayOrder order) {
        Objects.requireNonNull(order, "order must not be null");
        return new PaymentOrderResult(
                order.paymentId().value(), order.gatewayType(), order.providerOrderId(), order.paymentLink());
    }
}
