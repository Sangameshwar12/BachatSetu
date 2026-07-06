package in.bachatsetu.backend.group.domain.model;

import java.util.Objects;

/** Normalized descriptive text supplied by the group owner. */
public record GroupDescription(String value) {

    public GroupDescription {
        Objects.requireNonNull(value, "group description must not be null");
        value = value.strip();
    }

    public static GroupDescription empty() {
        return new GroupDescription("");
    }
}
