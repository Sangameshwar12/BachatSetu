package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.audit.AuditLogJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;

public interface AuditLogSpringDataRepository extends BaseJpaRepository<AuditLogJpaEntity> {
}
