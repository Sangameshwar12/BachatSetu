package in.bachatsetu.backend.payment.application.support;

import java.time.LocalDate;
import java.util.Objects;

/** A single contribution cycle's date boundaries, derived from a group's schedule. */
public record CycleWindow(int cycleNumber, LocalDate periodStart, LocalDate periodEnd) {

    public CycleWindow {
        if (cycleNumber < 1) {
            throw new IllegalArgumentException("cycle number must be at least 1");
        }
        Objects.requireNonNull(periodStart, "period start must not be null");
        Objects.requireNonNull(periodEnd, "period end must not be null");
        if (!periodEnd.isAfter(periodStart)) {
            throw new IllegalArgumentException("period end must be after period start");
        }
    }

    /** The date a member's contribution for this cycle is due — the first day of the period. */
    public LocalDate dueDate() {
        return periodStart;
    }
}
