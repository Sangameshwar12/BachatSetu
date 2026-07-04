package in.bachatsetu.backend.group.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.group.domain.event.GroupActivated;
import in.bachatsetu.backend.group.domain.exception.InvalidGroupStateException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SavingsGroupTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");

    @Test
    void activatesWhenMemberCapacityIsSatisfied() {
        AggregateId organizerId = AggregateId.newId();
        SavingsGroup group = newGroup(organizerId);
        group.pullDomainEvents();

        group.activate(10, organizerId, NOW.plusSeconds(60));

        assertThat(group.status()).isEqualTo(GroupStatus.ACTIVE);
        assertThat(group.version()).isEqualTo(1);
        assertThat(group.domainEvents()).singleElement().isInstanceOf(GroupActivated.class);
    }

    @Test
    void rejectsActivationOutsideMemberCapacity() {
        AggregateId organizerId = AggregateId.newId();
        SavingsGroup group = newGroup(organizerId);

        assertThatThrownBy(() -> group.activate(4, organizerId, NOW.plusSeconds(60)))
                .isInstanceOf(InvalidGroupStateException.class);
    }

    private SavingsGroup newGroup(AggregateId organizerId) {
        GroupRule rule = new GroupRule(
                AggregateId.newId(),
                new ContributionSchedule(
                        Money.inr(100_000), ContributionFrequency.MONTHLY, LocalDate.of(2026, 8, 1), 10),
                new MemberCapacity(10, 10),
                PayoutMethod.RANDOM_DRAW,
                false);
        return SavingsGroup.create(
                AggregateId.newId(),
                AggregateId.newId(),
                organizerId,
                new GroupCode("BS-001"),
                new GroupName("Bachat Circle"),
                GroupType.BHISHI,
                rule,
                NOW);
    }
}
