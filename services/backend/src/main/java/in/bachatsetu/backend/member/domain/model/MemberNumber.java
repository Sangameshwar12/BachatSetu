package in.bachatsetu.backend.member.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record MemberNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z0-9][A-Z0-9-]{2,31}$");

    public MemberNumber {
        Objects.requireNonNull(value, "member number must not be null");
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("member number format is invalid");
        }
    }
}
