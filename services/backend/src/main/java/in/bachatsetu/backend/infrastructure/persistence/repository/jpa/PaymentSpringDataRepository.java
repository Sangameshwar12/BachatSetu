package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.finance.PaymentJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentSpringDataRepository extends BaseJpaRepository<PaymentJpaEntity> {

    Optional<PaymentJpaEntity> findByTenantIdAndIdAndDeletedFalse(UUID tenantId, UUID id);

    Optional<PaymentJpaEntity> findFirstByTenantIdAndGroup_IdAndPayer_IdAndDeletedFalseOrderByCreatedAtDesc(
            UUID tenantId, UUID groupId, UUID payerId);

    List<PaymentJpaEntity> findAllByTenantIdAndGroup_IdAndStatusAndCreatedAtBetweenAndDeletedFalse(
            UUID tenantId, UUID groupId, PaymentStatus status, Instant from, Instant to);

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(PaymentStatus status);

    /** Per-tenant payment count, for Platform Operations tenant statistics only. */
    long countByTenantIdAndDeletedFalse(UUID tenantId);

    /** Platform-wide payment count in a window, for the Platform Operations dashboard only. */
    long countByCreatedAtBetween(Instant start, Instant end);

    Page<PaymentJpaEntity> findAllByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    Optional<PaymentJpaEntity> findByTenantIdAndReferenceAndDeletedFalse(UUID tenantId, String reference);

    Optional<PaymentJpaEntity> findByTenantIdAndIdempotencyKeyHashAndDeletedFalse(
            UUID tenantId, String idempotencyKeyHash);

    Optional<PaymentJpaEntity> findByProviderNameAndProviderPaymentReferenceAndDeletedFalse(
            String providerName, String providerPaymentReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentJpaEntity> findForUpdateByTenantIdAndReferenceAndDeletedFalse(
            UUID tenantId, String reference);

    @Query("SELECT COALESCE(SUM(p.amountPaise), 0) FROM PaymentJpaEntity p WHERE p.deleted = false")
    long sumAmountPaise();

    @Query("SELECT COALESCE(SUM(p.amountPaise), 0) FROM PaymentJpaEntity p "
            + "WHERE p.deleted = false AND p.status = :status")
    long sumAmountPaiseByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amountPaise), 0) FROM PaymentJpaEntity p "
            + "WHERE p.deleted = false AND p.tenantId = :tenantId AND p.status = :status")
    long sumAmountPaiseByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") PaymentStatus status);

    /**
     * One row per day since {@code since}: {@code [year, month, day, count, sum(amountPaise)]}, for platform
     * analytics only.
     */
    @Query("""
            SELECT EXTRACT(YEAR FROM p.createdAt), EXTRACT(MONTH FROM p.createdAt), EXTRACT(DAY FROM p.createdAt),
                   COUNT(p), COALESCE(SUM(p.amountPaise), 0)
              FROM PaymentJpaEntity p
             WHERE p.deleted = false
               AND p.createdAt >= :since
             GROUP BY EXTRACT(YEAR FROM p.createdAt), EXTRACT(MONTH FROM p.createdAt), EXTRACT(DAY FROM p.createdAt)
             ORDER BY EXTRACT(YEAR FROM p.createdAt), EXTRACT(MONTH FROM p.createdAt), EXTRACT(DAY FROM p.createdAt)
            """)
    List<Object[]> findDailyPaymentTrend(@Param("since") Instant since);
}
