package in.bachatsetu.backend.shared.domain;

import java.util.Objects;
import java.util.UUID;

public record AggregateId(UUID value) {

    public AggregateId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static AggregateId newId() {
        return new AggregateId(UUID.randomUUID());
    }

    public static AggregateId from(String value) {
        Objects.requireNonNull(value, "value must not be null");
        return new AggregateId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
