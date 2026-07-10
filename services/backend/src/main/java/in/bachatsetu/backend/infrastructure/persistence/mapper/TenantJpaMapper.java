package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.platform.TenantJpaEntity;
import in.bachatsetu.backend.platformoperations.domain.model.Tenant;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface TenantJpaMapper {

    default Tenant toDomain(TenantJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Tenant.rehydrate(
                JpaMappingSupport.id(entity.getId()), entity.getStatus(), entity.getSuspensionReason(),
                JpaMappingSupport.auditInfo(entity), entity.getVersion());
    }

    default TenantJpaEntity toEntity(Tenant domain) {
        if (domain == null) {
            return null;
        }
        return new TenantJpaEntity(domain.id().value(), domain.status(), domain.suspensionReason());
    }
}
