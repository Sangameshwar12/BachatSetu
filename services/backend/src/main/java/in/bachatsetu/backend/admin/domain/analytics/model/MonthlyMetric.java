package in.bachatsetu.backend.admin.domain.analytics.model;

import java.time.YearMonth;
import java.util.Objects;

/** One month's count within a trend (new groups per month, user registrations per month). */
public record MonthlyMetric(YearMonth month, long count) {

    public MonthlyMetric {
        Objects.requireNonNull(month, "month must not be null");
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative");
        }
    }
}
