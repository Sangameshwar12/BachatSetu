package in.bachatsetu.backend.admin.application.configuration.service;

import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import in.bachatsetu.backend.admin.domain.configuration.port.FeatureFlagRepository;
import java.util.Objects;

/**
 * Lightweight, read-only feature-flag lookup consulted on the request hot path (the HTTP-level feature-flag
 * enforcement filter) as well as internally. Deliberately has no {@code TransactionPort} indirection since
 * the underlying repository adapter is already {@code @Transactional(readOnly = true)} on its own.
 */
public final class FeatureFlagQueryService {

    private final FeatureFlagRepository repository;

    public FeatureFlagQueryService(FeatureFlagRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    /** A feature with no stored row is treated as enabled (fail open on missing configuration). */
    public boolean isEnabled(FeatureKey key) {
        Objects.requireNonNull(key, "key must not be null");
        return repository.findByKey(key).map(flag -> flag.enabled()).orElse(true);
    }
}
