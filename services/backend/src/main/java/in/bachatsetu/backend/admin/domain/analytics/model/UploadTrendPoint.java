package in.bachatsetu.backend.admin.domain.analytics.model;

import java.time.LocalDate;
import java.util.Objects;

/** One day's upload activity within the trailing trend window. */
public record UploadTrendPoint(LocalDate date, long count) {

    public UploadTrendPoint {
        Objects.requireNonNull(date, "date must not be null");
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative");
        }
    }
}
