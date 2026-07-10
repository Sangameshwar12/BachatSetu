package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import in.bachatsetu.backend.infrastructure.persistence.entity.config.FeatureFlagJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagSpringDataRepository extends JpaRepository<FeatureFlagJpaEntity, FeatureKey> {
}
