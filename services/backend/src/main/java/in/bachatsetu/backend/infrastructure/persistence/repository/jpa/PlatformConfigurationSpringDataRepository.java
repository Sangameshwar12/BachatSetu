package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.config.PlatformConfigurationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformConfigurationSpringDataRepository
        extends JpaRepository<PlatformConfigurationJpaEntity, Short> {
}
