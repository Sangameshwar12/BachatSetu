package in.bachatsetu.backend.admin.domain.analytics.model;

import java.time.LocalDate;
import java.util.Objects;

/** One day's payment activity within the trailing trend window. */
public record PaymentTrendPoint(LocalDate date, long count, long volumePaise) {

    public PaymentTrendPoint {
        Objects.requireNonNull(date, "date must not be null");
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative");
        }
        if (volumePaise < 0) {
            throw new IllegalArgumentException("volumePaise must not be negative");
        }
    }
}
