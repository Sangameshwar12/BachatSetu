package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.notification.NotificationJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationSpringDataRepository extends BaseJpaRepository<NotificationJpaEntity> {

    Optional<NotificationJpaEntity> findByTenantIdAndIdAndDeletedFalse(UUID tenantId, UUID id);

    Page<NotificationJpaEntity> findAllByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    boolean existsByTenantIdAndUser_IdAndCategoryAndCreatedAtGreaterThanEqualAndDeletedFalse(
            UUID tenantId, UUID userId, NotificationCategory category, Instant since);
}
