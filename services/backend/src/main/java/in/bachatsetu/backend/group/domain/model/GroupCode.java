package in.bachatsetu.backend.group.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** A normalized code used to identify a savings group to people. */
public record GroupCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z0-9][A-Z0-9-]{2,19}$");

    public GroupCode {
        Objects.requireNonNull(value, "group code must not be null");
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("group code format is invalid");
        }
    }
}
