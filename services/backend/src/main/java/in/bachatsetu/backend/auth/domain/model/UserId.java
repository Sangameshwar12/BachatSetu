package in.bachatsetu.backend.auth.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for an authentication user.
 *
 * @param value UUID value
 */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "user id must not be null");
    }

    /**
     * Generates a new user identifier.
     *
     * @return generated identifier
     */
    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }

    /**
     * Parses a user identifier.
     *
     * @param value UUID text
     * @return parsed identifier
     */
    public static UserId from(String value) {
        return new UserId(UUID.fromString(Objects.requireNonNull(value, "value must not be null")));
    }

    /**
     * Converts this typed identifier for shared aggregate infrastructure.
     *
     * @return shared aggregate identifier
     */
    public AggregateId toAggregateId() {
        return new AggregateId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
