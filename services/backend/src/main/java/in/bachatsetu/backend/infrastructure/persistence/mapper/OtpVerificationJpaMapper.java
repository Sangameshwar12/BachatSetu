package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.auth.domain.model.OtpHash;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.OtpVerificationJpaEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface OtpVerificationJpaMapper {

    default OtpVerification toDomain(OtpVerificationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return OtpVerification.rehydrate(
                JpaMappingSupport.id(entity.getId()),
                new UserId(entity.getUser().getId()),
                OtpHash.encoded(entity.getHash()),
                entity.getPurpose(),
                entity.getGeneratedAt(),
                entity.getExpiresAt(),
                entity.getStatus(),
                entity.getVerificationAttempts(),
                entity.getResendCount(),
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default OtpVerificationJpaEntity toEntity(
            OtpVerification domain,
            @Context JpaReferenceProvider references) {
        if (domain == null) {
            return null;
        }
        return new OtpVerificationJpaEntity(
                domain.id().value(),
                references.user(domain.userId().toAggregateId()),
                domain.hash().value(),
                domain.purpose(),
                domain.generatedAt(),
                domain.expiresAt(),
                domain.status(),
                domain.verificationAttempts(),
                domain.resendCount());
    }
}
