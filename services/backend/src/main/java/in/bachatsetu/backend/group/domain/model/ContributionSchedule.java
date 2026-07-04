package in.bachatsetu.backend.group.domain.model;

import in.bachatsetu.backend.shared.domain.Money;
import java.time.LocalDate;
import java.util.Objects;

public record ContributionSchedule(
        Money contributionAmount,
        ContributionFrequency frequency,
        LocalDate startDate,
        int cycleCount) {

    public ContributionSchedule {
        Objects.requireNonNull(contributionAmount, "contributionAmount must not be null");
        Objects.requireNonNull(frequency, "frequency must not be null");
        Objects.requireNonNull(startDate, "startDate must not be null");
        if (!contributionAmount.isPositive()) {
            throw new IllegalArgumentException("contributionAmount must be positive");
        }
        if (cycleCount < 1 || cycleCount > 120) {
            throw new IllegalArgumentException("cycleCount must be between 1 and 120");
        }
    }
}
