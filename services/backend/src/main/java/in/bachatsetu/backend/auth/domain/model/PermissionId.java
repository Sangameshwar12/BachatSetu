package in.bachatsetu.backend.auth.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for a permission.
 *
 * @param value UUID value
 */
public record PermissionId(UUID value) {

    public PermissionId {
        Objects.requireNonNull(value, "permission id must not be null");
    }

    public static PermissionId newId() {
        return new PermissionId(UUID.randomUUID());
    }

    public static PermissionId from(String value) {
        return new PermissionId(UUID.fromString(Objects.requireNonNull(value, "value must not be null")));
    }

    public AggregateId toAggregateId() {
        return new AggregateId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
