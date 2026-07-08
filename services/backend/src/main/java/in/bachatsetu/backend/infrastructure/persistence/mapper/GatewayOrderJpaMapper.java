package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.finance.GatewayOrderJpaEntity;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayOrder;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface GatewayOrderJpaMapper {

    default GatewayOrder toDomain(GatewayOrderJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new GatewayOrder(
                JpaMappingSupport.id(entity.getId()),
                JpaMappingSupport.id(entity.getTenantId()),
                JpaMappingSupport.id(entity.getPayment().getId()),
                entity.getGatewayType(),
                entity.getProviderOrderId(),
                entity.getPaymentLink(),
                entity.getProviderStatus(),
                entity.getProviderRefundId(),
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default GatewayOrderJpaEntity toEntity(GatewayOrder domain, @Context JpaReferenceProvider references) {
        if (domain == null) {
            return null;
        }
        return new GatewayOrderJpaEntity(
                domain.id().value(),
                domain.tenantId().value(),
                references.payment(domain.paymentId()),
                domain.gatewayType(),
                domain.providerOrderId(),
                domain.paymentLink(),
                domain.providerStatus(),
                domain.providerRefundId());
    }
}
