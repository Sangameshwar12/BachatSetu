package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.finance.PaymentJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;

public interface PaymentSpringDataRepository extends BaseJpaRepository<PaymentJpaEntity> {

    Optional<PaymentJpaEntity> findByTenantIdAndIdAndDeletedFalse(UUID tenantId, UUID id);

    Page<PaymentJpaEntity> findAllByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    Optional<PaymentJpaEntity> findByTenantIdAndReferenceAndDeletedFalse(UUID tenantId, String reference);

    Optional<PaymentJpaEntity> findByTenantIdAndIdempotencyKeyHashAndDeletedFalse(
            UUID tenantId, String idempotencyKeyHash);

    Optional<PaymentJpaEntity> findByProviderNameAndProviderPaymentReferenceAndDeletedFalse(
            String providerName, String providerPaymentReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentJpaEntity> findForUpdateByTenantIdAndReferenceAndDeletedFalse(
            UUID tenantId, String reference);
}
