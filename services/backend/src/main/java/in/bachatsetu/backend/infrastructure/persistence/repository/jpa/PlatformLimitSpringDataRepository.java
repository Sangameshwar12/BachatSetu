package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.admin.domain.configuration.model.LimitKey;
import in.bachatsetu.backend.infrastructure.persistence.entity.config.PlatformLimitJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformLimitSpringDataRepository extends JpaRepository<PlatformLimitJpaEntity, LimitKey> {
}
