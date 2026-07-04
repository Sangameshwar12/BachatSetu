package in.bachatsetu.backend.payment.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record PaymentReference(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z0-9][A-Z0-9-]{7,39}$");

    public PaymentReference {
        Objects.requireNonNull(value, "payment reference must not be null");
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("payment reference format is invalid");
        }
    }
}
