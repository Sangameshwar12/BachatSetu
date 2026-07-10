package in.bachatsetu.backend.admin.domain.configuration.port;

import in.bachatsetu.backend.admin.domain.configuration.model.LimitKey;
import in.bachatsetu.backend.admin.domain.configuration.model.PlatformLimit;
import java.util.List;
import java.util.Optional;

/** Loads and persists configurable platform-wide ceilings. */
public interface PlatformLimitRepository {

    List<PlatformLimit> findAll();

    Optional<PlatformLimit> findByKey(LimitKey key);

    void save(PlatformLimit limit);
}
