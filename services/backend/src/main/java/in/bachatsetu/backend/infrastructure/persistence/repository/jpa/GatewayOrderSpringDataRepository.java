package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.finance.GatewayOrderJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import java.util.Optional;
import java.util.UUID;

public interface GatewayOrderSpringDataRepository extends BaseJpaRepository<GatewayOrderJpaEntity> {

    Optional<GatewayOrderJpaEntity> findByTenantIdAndPayment_IdAndDeletedFalse(UUID tenantId, UUID paymentId);

    Optional<GatewayOrderJpaEntity> findByGatewayTypeAndProviderOrderIdAndDeletedFalse(
            GatewayType gatewayType, String providerOrderId);
}
