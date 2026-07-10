package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.notification.NotificationJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface NotificationSpringDataRepository extends BaseJpaRepository<NotificationJpaEntity> {

    Optional<NotificationJpaEntity> findByTenantIdAndIdAndDeletedFalse(UUID tenantId, UUID id);

    long countByDeletedFalse();

    /** Per-tenant notification count, for Platform Operations tenant statistics only. */
    long countByTenantIdAndDeletedFalse(UUID tenantId);

    /** Platform-wide notification count in a window, for the Platform Operations dashboard only. */
    long countByCreatedAtBetween(Instant start, Instant end);

    Page<NotificationJpaEntity> findAllByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    boolean existsByTenantIdAndUser_IdAndCategoryAndCreatedAtGreaterThanEqualAndDeletedFalse(
            UUID tenantId, UUID userId, NotificationCategory category, Instant since);

    List<NotificationJpaEntity> findTop5ByTenantIdAndUser_IdAndDeletedFalseOrderByCreatedAtDesc(
            UUID tenantId, UUID userId);

    /** One row per delivery status: {@code [NotificationStatus, count]}, for platform analytics only. */
    @Query("SELECT n.status, COUNT(n) FROM NotificationJpaEntity n WHERE n.deleted = false GROUP BY n.status")
    List<Object[]> findStatusDistribution();

    /** One row per notification category: {@code [NotificationCategory, count]}, for platform analytics only. */
    @Query("SELECT n.category, COUNT(n) FROM NotificationJpaEntity n WHERE n.deleted = false GROUP BY n.category")
    List<Object[]> findCategoryDistribution();
}
