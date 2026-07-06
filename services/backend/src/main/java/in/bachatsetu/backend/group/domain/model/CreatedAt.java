package in.bachatsetu.backend.group.domain.model;

import java.time.Instant;
import java.util.Objects;

/** Creation timestamp of a savings group or membership. */
public record CreatedAt(Instant value) {

    public CreatedAt {
        Objects.requireNonNull(value, "created at must not be null");
    }
}
