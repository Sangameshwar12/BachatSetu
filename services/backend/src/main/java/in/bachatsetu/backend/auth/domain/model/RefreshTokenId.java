package in.bachatsetu.backend.auth.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for refresh-token lifecycle state.
 *
 * @param value UUID value
 */
public record RefreshTokenId(UUID value) {

    public RefreshTokenId {
        Objects.requireNonNull(value, "refresh token id must not be null");
    }

    public static RefreshTokenId newId() {
        return new RefreshTokenId(UUID.randomUUID());
    }

    public static RefreshTokenId from(String value) {
        return new RefreshTokenId(UUID.fromString(Objects.requireNonNull(value, "value must not be null")));
    }

    public AggregateId toAggregateId() {
        return new AggregateId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
