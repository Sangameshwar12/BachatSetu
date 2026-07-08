package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.notification.NotificationJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationSpringDataRepository extends BaseJpaRepository<NotificationJpaEntity> {

    Optional<NotificationJpaEntity> findByTenantIdAndIdAndDeletedFalse(UUID tenantId, UUID id);

    Page<NotificationJpaEntity> findAllByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
}
