package in.bachatsetu.backend.platformoperations.domain.model;

import java.util.Objects;

public record ComponentHealth(String name, HealthStatus status, String detail) {

    public ComponentHealth {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
