package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.finance.ReceiptJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReceiptSpringDataRepository extends BaseJpaRepository<ReceiptJpaEntity> {

    Optional<ReceiptJpaEntity> findByTenantIdAndIdAndDeletedFalse(UUID tenantId, UUID id);

    Page<ReceiptJpaEntity> findAllByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    Optional<ReceiptJpaEntity> findByTenantIdAndNumberAndDeletedFalse(UUID tenantId, String number);

    Optional<ReceiptJpaEntity> findByPayment_IdAndDeletedFalse(UUID paymentId);
}
