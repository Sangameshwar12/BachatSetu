package in.bachatsetu.backend.group.domain.model;

import java.util.Objects;

public record GroupName(String value) {

    public GroupName {
        Objects.requireNonNull(value, "group name must not be null");
        value = value.strip();
        if (value.length() < 3 || value.length() > 120) {
            throw new IllegalArgumentException("group name must contain 3 to 120 characters");
        }
    }
}
