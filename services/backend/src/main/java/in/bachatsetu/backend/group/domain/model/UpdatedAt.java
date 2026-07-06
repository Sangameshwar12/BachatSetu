package in.bachatsetu.backend.group.domain.model;

import java.time.Instant;
import java.util.Objects;

/** Timestamp of the most recent aggregate change. */
public record UpdatedAt(Instant value) {

    public UpdatedAt {
        Objects.requireNonNull(value, "updated at must not be null");
    }
}
