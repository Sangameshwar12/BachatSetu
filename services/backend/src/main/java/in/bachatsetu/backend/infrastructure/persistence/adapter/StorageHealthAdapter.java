package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.platformoperations.domain.model.ComponentHealth;
import in.bachatsetu.backend.platformoperations.domain.model.HealthStatus;
import in.bachatsetu.backend.platformoperations.domain.port.StorageHealthPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Reports storage health from its own feature flag and configured provider. The cloud provider adapters in
 * this codebase are simulated (no live SDK calls — see their own class Javadocs), so this is a configuration
 * check rather than a live connectivity probe for anything but the {@code LOCAL} provider.
 */
@Component
@ConditionalOnPersistenceRepositories
public class StorageHealthAdapter implements StorageHealthPort {

    private final boolean enabled;
    private final String provider;

    public StorageHealthAdapter(
            @Value("${bachatsetu.storage.enabled:true}") boolean enabled,
            @Value("${bachatsetu.storage.default-provider:LOCAL}") String provider) {
        this.enabled = enabled;
        this.provider = provider;
    }

    @Override
    public ComponentHealth check() {
        if (!enabled) {
            return new ComponentHealth("storage", HealthStatus.DOWN, "storage module disabled");
        }
        return new ComponentHealth("storage", HealthStatus.UP, "configured provider: " + provider);
    }
}
