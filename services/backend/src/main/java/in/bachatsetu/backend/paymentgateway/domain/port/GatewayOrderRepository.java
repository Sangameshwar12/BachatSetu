package in.bachatsetu.backend.paymentgateway.domain.port;

import in.bachatsetu.backend.paymentgateway.domain.model.GatewayOrder;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;

public interface GatewayOrderRepository {

    Optional<GatewayOrder> findByPaymentId(AggregateId tenantId, AggregateId paymentId);

    /** Cross-tenant: a webhook identifies its order only by the provider's own id, never a tenant. */
    Optional<GatewayOrder> findByProviderOrderId(GatewayType gatewayType, String providerOrderId);

    void save(GatewayOrder order);
}
