package in.bachatsetu.backend.auth.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Stable identifier for one authenticated device session. */
public record TokenSessionId(UUID value) {

    public TokenSessionId {
        Objects.requireNonNull(value, "token session id must not be null");
    }

    public static TokenSessionId newId() {
        return new TokenSessionId(UUID.randomUUID());
    }

    public static TokenSessionId from(String value) {
        return new TokenSessionId(UUID.fromString(Objects.requireNonNull(value, "value must not be null")));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
