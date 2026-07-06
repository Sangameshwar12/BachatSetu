package in.bachatsetu.backend.group.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Strongly typed identity of a savings group aggregate. */
public record GroupId(AggregateId value) {

    public GroupId {
        Objects.requireNonNull(value, "group id must not be null");
    }

    public static GroupId newId() {
        return new GroupId(AggregateId.newId());
    }

    public static GroupId from(String value) {
        return new GroupId(AggregateId.from(value));
    }
}
