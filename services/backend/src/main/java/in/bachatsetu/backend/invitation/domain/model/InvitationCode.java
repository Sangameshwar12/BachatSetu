package in.bachatsetu.backend.invitation.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/** Short, human-typeable invitation code (e.g. shared verbally or over chat). */
public record InvitationCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z0-9]{6,12}$");

    public InvitationCode {
        Objects.requireNonNull(value, "invitation code must not be null");
        value = value.strip().toUpperCase(java.util.Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("invitation code must be 6-12 uppercase letters or digits");
        }
    }
}
