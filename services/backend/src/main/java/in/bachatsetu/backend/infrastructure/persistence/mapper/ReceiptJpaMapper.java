package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.finance.ReceiptJpaEntity;
import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.model.ReceiptDescription;
import in.bachatsetu.backend.receipt.domain.model.ReceiptLine;
import in.bachatsetu.backend.receipt.domain.model.ReceiptNumber;
import in.bachatsetu.backend.receipt.domain.model.ReceiptType;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.ZoneOffset;
import java.util.List;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface ReceiptJpaMapper {

    default Receipt toDomain(ReceiptJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        Money total = new Money(entity.getAmountPaise(), JpaMappingSupport.currency(entity.getCurrencyCode()));
        ReceiptLine line = new ReceiptLine(
                JpaMappingSupport.id(entity.getId()),
                ReceiptType.CONTRIBUTION,
                new ReceiptDescription("Payment receipt"),
                total);
        return new Receipt(
                JpaMappingSupport.id(entity.getId()),
                JpaMappingSupport.id(entity.getTenantId()),
                JpaMappingSupport.id(entity.getPayment().getId()),
                JpaMappingSupport.id(entity.getUser().getId()),
                new ReceiptNumber(entity.getNumber()),
                List.of(line),
                entity.getStatus(),
                entity.getCancellationReason(),
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default ReceiptJpaEntity toEntity(Receipt domain, @Context JpaReferenceProvider references) {
        if (domain == null) {
            return null;
        }
        return new ReceiptJpaEntity(
                domain.id().value(),
                domain.tenantId().value(),
                references.payment(domain.paymentId()),
                references.user(domain.memberId()),
                domain.number().value(),
                domain.auditInfo().createdAt().atZone(ZoneOffset.UTC).toLocalDate(),
                domain.total().minorUnits(),
                domain.total().currency().getCurrencyCode(),
                domain.status(),
                domain.cancellationReason());
    }
}
