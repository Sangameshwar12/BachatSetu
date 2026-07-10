package in.bachatsetu.backend.admin.domain.configuration.port;

import in.bachatsetu.backend.admin.domain.configuration.model.FeatureFlag;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import java.util.List;
import java.util.Optional;

/** Loads and persists per-feature enable/disable state. */
public interface FeatureFlagRepository {

    List<FeatureFlag> findAll();

    Optional<FeatureFlag> findByKey(FeatureKey key);

    void save(FeatureFlag flag);
}
