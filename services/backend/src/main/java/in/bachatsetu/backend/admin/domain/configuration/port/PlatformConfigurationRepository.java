package in.bachatsetu.backend.admin.domain.configuration.port;

import in.bachatsetu.backend.admin.domain.configuration.model.PlatformConfiguration;

/** Loads and persists the single platform configuration singleton. */
public interface PlatformConfigurationRepository {

    PlatformConfiguration find();

    void save(PlatformConfiguration configuration);
}
