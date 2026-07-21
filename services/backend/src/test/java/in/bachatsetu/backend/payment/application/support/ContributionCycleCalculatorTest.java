package in.bachatsetu.backend.payment.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.group.domain.model.ContributionFrequency;
import in.bachatsetu.backend.group.domain.model.ContributionSchedule;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ContributionCycleCalculatorTest {

    private static final LocalDate START = LocalDate.of(2026, 8, 1);

    @Test
    void reportsNoCycleBeforeTheScheduleStarts() {
        ContributionSchedule schedule = monthlySchedule(12);

        Optional<CycleWindow> cycle = ContributionCycleCalculator.currentCycle(schedule, START.minusDays(1));

        assertThat(cycle).isEmpty();
    }

    @Test
    void firstCycleStartsOnTheScheduleStartDate() {
        ContributionSchedule schedule = monthlySchedule(12);

        CycleWindow cycle = ContributionCycleCalculator.currentCycle(schedule, START).orElseThrow();

        assertThat(cycle.cycleNumber()).isEqualTo(1);
        assertThat(cycle.periodStart()).isEqualTo(START);
        assertThat(cycle.periodEnd()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(cycle.dueDate()).isEqualTo(START);
    }

    @Test
    void staysInTheFirstCycleUntilThePeriodEnds() {
        ContributionSchedule schedule = monthlySchedule(12);

        CycleWindow cycle = ContributionCycleCalculator.currentCycle(schedule, LocalDate.of(2026, 8, 31))
                .orElseThrow();

        assertThat(cycle.cycleNumber()).isEqualTo(1);
    }

    @Test
    void movesToTheSecondCycleOnItsFirstDay() {
        ContributionSchedule schedule = monthlySchedule(12);

        CycleWindow cycle = ContributionCycleCalculator.currentCycle(schedule, LocalDate.of(2026, 9, 1))
                .orElseThrow();

        assertThat(cycle.cycleNumber()).isEqualTo(2);
        assertThat(cycle.periodStart()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(cycle.periodEnd()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void reportsNoCycleOnceEveryScheduledCycleHasElapsed() {
        ContributionSchedule schedule = monthlySchedule(12);

        Optional<CycleWindow> cycle =
                ContributionCycleCalculator.currentCycle(schedule, LocalDate.of(2027, 8, 1));

        assertThat(cycle).isEmpty();
    }

    @Test
    void staysInTheLastCycleUntilItElapses() {
        ContributionSchedule schedule = monthlySchedule(12);

        CycleWindow cycle = ContributionCycleCalculator.currentCycle(schedule, LocalDate.of(2027, 7, 31))
                .orElseThrow();

        assertThat(cycle.cycleNumber()).isEqualTo(12);
    }

    @Test
    void computesWeeklyCycles() {
        ContributionSchedule schedule = new ContributionSchedule(Money.inr(50_000), ContributionFrequency.WEEKLY,
                START, 4);

        CycleWindow secondWeek = ContributionCycleCalculator.currentCycle(schedule, START.plusWeeks(1))
                .orElseThrow();

        assertThat(secondWeek.cycleNumber()).isEqualTo(2);
        assertThat(secondWeek.periodStart()).isEqualTo(START.plusWeeks(1));
        assertThat(secondWeek.periodEnd()).isEqualTo(START.plusWeeks(2));
    }

    @Test
    void computesQuarterlyCycles() {
        ContributionSchedule schedule = new ContributionSchedule(Money.inr(300_000),
                ContributionFrequency.QUARTERLY, START, 4);

        CycleWindow secondQuarter = ContributionCycleCalculator.currentCycle(schedule, START.plusMonths(3))
                .orElseThrow();

        assertThat(secondQuarter.cycleNumber()).isEqualTo(2);
        assertThat(secondQuarter.periodStart()).isEqualTo(START.plusMonths(3));
        assertThat(secondQuarter.periodEnd()).isEqualTo(START.plusMonths(6));
    }

    @Test
    void rejectsNullInputs() {
        ContributionSchedule schedule = monthlySchedule(12);
        assertThatThrownBy(() -> ContributionCycleCalculator.currentCycle(null, START))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ContributionCycleCalculator.currentCycle(schedule, null))
                .isInstanceOf(NullPointerException.class);
    }

    private ContributionSchedule monthlySchedule(int cycleCount) {
        return new ContributionSchedule(Money.inr(100_000), ContributionFrequency.MONTHLY, START, cycleCount);
    }
}
