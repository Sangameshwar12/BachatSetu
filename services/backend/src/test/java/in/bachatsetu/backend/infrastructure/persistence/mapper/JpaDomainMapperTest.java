package in.bachatsetu.backend.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.exception.PersistenceMappingException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Email;
import in.bachatsetu.backend.user.domain.model.PersonName;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import in.bachatsetu.backend.user.domain.model.UserContact;
import in.bachatsetu.backend.user.domain.model.UserProfile;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class JpaDomainMapperTest {

    private final UserJpaMapper mapper = Mappers.getMapper(UserJpaMapper.class);

    @Test
    void mapsAUserAggregateToItsJpaRepresentation() {
        AggregateId actorId = AggregateId.newId();
        UserProfile user = UserProfile.register(
                AggregateId.newId(),
                new PersonName("Asha", "Patil"),
                new UserContact(new Email("asha@example.in"), null),
                PreferredLanguage.MARATHI,
                actorId,
                Instant.parse("2026-07-04T10:00:00Z"));
        UUID tenantId = UUID.randomUUID();

        UserJpaEntity entity = mapper.toEntity(user, tenantId);

        assertThat(entity.getId()).isEqualTo(user.id().value());
        assertThat(entity.getTenantId()).isEqualTo(tenantId);
        assertThat(entity.getGivenName()).isEqualTo("Asha");
        assertThat(entity.getEmail()).isEqualTo("asha@example.in");
    }

    @Test
    void refusesToRehydrateAnEntityWithoutJpaAuditMetadata() {
        UserJpaEntity entity = new UserJpaEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Asha",
                "Patil",
                "asha@example.in",
                null,
                in.bachatsetu.backend.user.domain.model.UserStatus.ACTIVE,
                PreferredLanguage.MARATHI);

        assertThatThrownBy(() -> mapper.toDomain(entity))
                .isInstanceOf(PersistenceMappingException.class)
                .hasMessageContaining("audit metadata");
    }
}
