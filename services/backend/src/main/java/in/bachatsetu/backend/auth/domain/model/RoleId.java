package in.bachatsetu.backend.auth.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for a role.
 *
 * @param value UUID value
 */
public record RoleId(UUID value) {

    public RoleId {
        Objects.requireNonNull(value, "role id must not be null");
    }

    public static RoleId newId() {
        return new RoleId(UUID.randomUUID());
    }

    public static RoleId from(String value) {
        return new RoleId(UUID.fromString(Objects.requireNonNull(value, "value must not be null")));
    }

    public AggregateId toAggregateId() {
        return new AggregateId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
