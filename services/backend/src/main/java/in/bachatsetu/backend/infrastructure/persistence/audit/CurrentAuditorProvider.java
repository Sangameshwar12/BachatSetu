package in.bachatsetu.backend.infrastructure.persistence.audit;

import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
public interface CurrentAuditorProvider {

    Optional<UUID> currentAuditorId();
}
