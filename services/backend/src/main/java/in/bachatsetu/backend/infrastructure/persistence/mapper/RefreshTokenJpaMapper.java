package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.auth.domain.model.RefreshToken;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.RefreshTokenJpaEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface RefreshTokenJpaMapper {

    default RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return RefreshToken.rehydrate(
                new RefreshTokenId(entity.getId()),
                new UserId(entity.getUser().getId()),
                entity.getIssuedAt(),
                entity.getExpiresAt(),
                entity.getStatus(),
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default RefreshTokenJpaEntity toEntity(
            RefreshToken domain,
            @Context JpaReferenceProvider references) {
        if (domain == null) {
            return null;
        }
        return new RefreshTokenJpaEntity(
                domain.refreshTokenId().value(),
                references.user(domain.userId().toAggregateId()),
                domain.issuedAt(),
                domain.expiresAt(),
                domain.status());
    }
}
