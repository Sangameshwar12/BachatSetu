package in.bachatsetu.backend.platformoperations.domain.port;

import in.bachatsetu.backend.platformoperations.domain.model.ComponentHealth;

@FunctionalInterface
public interface DatabaseHealthPort {

    ComponentHealth check();
}
