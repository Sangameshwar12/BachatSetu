package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.finance.ReceiptJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptSpringDataRepository extends BaseJpaRepository<ReceiptJpaEntity> {

    Optional<ReceiptJpaEntity> findByTenantIdAndNumberAndDeletedFalse(UUID tenantId, String number);

    Optional<ReceiptJpaEntity> findByPayment_IdAndDeletedFalse(UUID paymentId);
}
