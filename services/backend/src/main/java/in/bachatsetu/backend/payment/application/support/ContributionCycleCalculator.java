package in.bachatsetu.backend.payment.application.support;

import in.bachatsetu.backend.group.domain.model.ContributionFrequency;
import in.bachatsetu.backend.group.domain.model.ContributionSchedule;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Derives which contribution cycle a date falls into from a group's own {@link ContributionSchedule},
 * without requiring any persisted cycle/installment record.
 *
 * <p>A cycle's due date is defined as the first day of its period: contributions are expected at the
 * start of each period (the point at which the group starts collecting for that cycle), matching how a
 * Bhishi/chit-fund circle collects before its draw or payout.
 */
public final class ContributionCycleCalculator {

    private ContributionCycleCalculator() {
    }

    /**
     * The cycle containing {@code today}, or empty if contributions haven't started yet
     * ({@code today} precedes the schedule's start date) or the schedule's cycles are exhausted.
     */
    public static Optional<CycleWindow> currentCycle(ContributionSchedule schedule, LocalDate today) {
        Objects.requireNonNull(schedule, "schedule must not be null");
        Objects.requireNonNull(today, "today must not be null");
        if (today.isBefore(schedule.startDate())) {
            return Optional.empty();
        }
        int cycleIndex = 0;
        LocalDate periodStart = schedule.startDate();
        while (true) {
            LocalDate periodEnd = advance(periodStart, schedule.frequency());
            if (today.isBefore(periodEnd)) {
                return Optional.of(new CycleWindow(cycleIndex + 1, periodStart, periodEnd));
            }
            if (cycleIndex + 1 >= schedule.cycleCount()) {
                return Optional.empty();
            }
            periodStart = periodEnd;
            cycleIndex++;
        }
    }

    private static LocalDate advance(LocalDate date, ContributionFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case QUARTERLY -> date.plusMonths(3);
        };
    }
}
