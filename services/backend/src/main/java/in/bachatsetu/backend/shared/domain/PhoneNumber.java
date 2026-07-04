package in.bachatsetu.backend.shared.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record PhoneNumber(String value) {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    public PhoneNumber {
        Objects.requireNonNull(value, "phone number must not be null");
        value = value.strip();
        if (!E164.matcher(value).matches()) {
            throw new IllegalArgumentException("phone number must use E.164 format");
        }
    }
}
