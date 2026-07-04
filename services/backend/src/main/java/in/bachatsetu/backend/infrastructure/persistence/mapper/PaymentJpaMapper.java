package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.finance.PaymentJpaEntity;
import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentReference;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.List;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface PaymentJpaMapper {

    default Payment toDomain(PaymentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Payment(
                JpaMappingSupport.id(entity.getId()),
                JpaMappingSupport.id(entity.getTenantId()),
                JpaMappingSupport.id(entity.getGroup().getId()),
                JpaMappingSupport.id(entity.getPayer().getId()),
                new PaymentReference(entity.getReference()),
                new IdempotencyKey(entity.getIdempotencyKeyHash()),
                new Money(entity.getAmountPaise(), JpaMappingSupport.currency(entity.getCurrencyCode())),
                entity.getMethod(),
                entity.getStatus(),
                entity.getReconciliationStatus(),
                List.of(),
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default PaymentJpaEntity toEntity(Payment domain, @Context JpaReferenceProvider references) {
        if (domain == null) {
            return null;
        }
        return new PaymentJpaEntity(
                domain.id().value(),
                domain.tenantId().value(),
                references.user(domain.memberId()),
                references.group(domain.groupId()),
                domain.reference().value(),
                domain.amount().minorUnits(),
                domain.amount().currency().getCurrencyCode(),
                domain.method(),
                domain.status(),
                domain.idempotencyKey().value(),
                null,
                null,
                domain.reconciliationStatus());
    }
}
