package in.bachatsetu.backend.receipt.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record ReceiptNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z0-9][A-Z0-9/-]{7,39}$");

    public ReceiptNumber {
        Objects.requireNonNull(value, "receipt number must not be null");
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("receipt number format is invalid");
        }
    }
}
