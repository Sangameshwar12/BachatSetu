package in.bachatsetu.backend.group.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Strongly typed identity of the user who owns a savings group. */
public record OwnerId(AggregateId value) {

    public OwnerId {
        Objects.requireNonNull(value, "owner id must not be null");
    }
}
