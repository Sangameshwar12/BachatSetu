package in.bachatsetu.backend.auth.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Canonical Indian mobile number in {@code +91XXXXXXXXXX} form.
 *
 * @param value canonical mobile number
 */
public record MobileNumber(String value) {

    private static final Pattern INDIAN_MOBILE = Pattern.compile("^\\+91[6-9]\\d{9}$");

    public MobileNumber {
        Objects.requireNonNull(value, "mobile number must not be null");
        value = value.strip();
        if (!INDIAN_MOBILE.matcher(value).matches()) {
            throw new IllegalArgumentException("mobile number must be an Indian number in +91 format");
        }
    }

    public static MobileNumber of(String value) {
        return new MobileNumber(value);
    }
}
