package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.exception.PersistenceMappingException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import java.util.Currency;

final class JpaMappingSupport {

    private JpaMappingSupport() {
    }

    static AggregateId id(java.util.UUID value) {
        return value == null ? null : new AggregateId(value);
    }

    static Currency currency(String code) {
        if (code == null) {
            throw new PersistenceMappingException("persisted currency code must not be null");
        }
        try {
            return Currency.getInstance(code);
        } catch (IllegalArgumentException exception) {
            throw new PersistenceMappingException("invalid persisted currency code", exception);
        }
    }

    static AuditInfo auditInfo(BaseJpaEntity entity) {
        if (entity.getCreatedAt() == null || entity.getUpdatedAt() == null
                || entity.getCreatedBy() == null || entity.getUpdatedBy() == null) {
            throw new PersistenceMappingException("persisted entity is missing audit metadata");
        }
        return new AuditInfo(
                entity.getCreatedAt(),
                id(entity.getCreatedBy()),
                entity.getUpdatedAt(),
                id(entity.getUpdatedBy()));
    }
}
