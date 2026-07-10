package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.exception.PersistenceMappingException;
import in.bachatsetu.backend.shared.domain.Email;
import in.bachatsetu.backend.shared.domain.PhoneNumber;
import in.bachatsetu.backend.user.domain.model.PersonName;
import in.bachatsetu.backend.user.domain.model.UserContact;
import in.bachatsetu.backend.user.domain.model.UserProfile;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface UserJpaMapper {

    default UserProfile toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        Email email = entity.getEmail() == null ? null : new Email(entity.getEmail());
        PhoneNumber phone = entity.getPhoneNumber() == null ? null : new PhoneNumber(entity.getPhoneNumber());
        if (email == null && phone == null) {
            throw new PersistenceMappingException("persisted user has no contact method");
        }
        in.bachatsetu.backend.shared.domain.AggregateId photoFileId = entity.getPhotoFileId() == null
                ? null
                : new in.bachatsetu.backend.shared.domain.AggregateId(entity.getPhotoFileId());
        return new UserProfile(
                JpaMappingSupport.id(entity.getId()),
                new PersonName(entity.getGivenName(), entity.getFamilyName()),
                new UserContact(email, phone),
                null,
                entity.getPreferredLanguage(),
                entity.getStatus(),
                List.of(),
                entity.getCity(),
                entity.getState(),
                photoFileId,
                entity.isNotificationsEnabled(),
                entity.isOnboarded(),
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default UserJpaEntity toEntity(UserProfile domain, java.util.UUID tenantId) {
        if (domain == null) {
            return null;
        }
        String email = domain.contact().email() == null ? null : domain.contact().email().value();
        String phone = domain.contact().phoneNumber() == null ? null : domain.contact().phoneNumber().value();
        UserJpaEntity entity = new UserJpaEntity(
                domain.id().value(),
                tenantId,
                domain.name().givenName(),
                domain.name().familyName(),
                email,
                phone,
                domain.status(),
                domain.preferredLanguage());
        if (domain.onboarded()) {
            entity.completeOnboarding(
                    domain.city(), domain.state(),
                    domain.photoFileId() == null ? null : domain.photoFileId().value(),
                    domain.notificationsEnabled());
        }
        return entity;
    }
}
