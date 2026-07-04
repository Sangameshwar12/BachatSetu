package in.bachatsetu.backend.infrastructure.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

class BaseJpaEntityTest {

    @Test
    void comparesAssignedIdentityWithinTheSameEntityType() {
        UUID id = UUID.randomUUID();

        assertThat(new TestEntity(id)).isEqualTo(new TestEntity(id));
        assertThat(new TestEntity(id)).isNotEqualTo(new TestEntity(UUID.randomUUID()));
    }

    @Test
    void declaresMappedSuperclassAuditingMetadata() {
        assertThat(BaseJpaEntity.class).hasAnnotation(MappedSuperclass.class);
        EntityListeners listeners = BaseJpaEntity.class.getAnnotation(EntityListeners.class);

        assertThat(listeners).isNotNull();
        assertThat(listeners.value()).contains(AuditingEntityListener.class);
    }

    @Test
    void recordsAndRestoresSoftDeletionMetadata() {
        TestEntity entity = new TestEntity(UUID.randomUUID());
        UUID actorId = UUID.randomUUID();
        Instant deletedAt = Instant.parse("2026-07-04T10:00:00Z");

        entity.delete(actorId, deletedAt);

        assertThat(entity.isDeleted()).isTrue();
        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(entity.getDeletedBy()).isEqualTo(actorId);

        entity.restoreEntity();

        assertThat(entity.isDeleted()).isFalse();
        assertThat(entity.getDeletedAt()).isNull();
        assertThat(entity.getDeletedBy()).isNull();
    }

    private static final class TestEntity extends BaseJpaEntity {

        private TestEntity(UUID id) {
            super(id);
        }

        private void delete(UUID actorId, Instant deletedAt) {
            markDeleted(actorId, deletedAt);
        }

        private void restoreEntity() {
            restore();
        }
    }
}
