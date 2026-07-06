package in.bachatsetu.backend.group.domain;

import in.bachatsetu.backend.group.domain.model.ContributionFrequency;
import in.bachatsetu.backend.group.domain.model.ContributionSchedule;
import in.bachatsetu.backend.group.domain.model.CreatedAt;
import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.GroupDescription;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.GroupName;
import in.bachatsetu.backend.group.domain.model.GroupRule;
import in.bachatsetu.backend.group.domain.model.GroupType;
import in.bachatsetu.backend.group.domain.model.MemberCapacity;
import in.bachatsetu.backend.group.domain.model.OwnerId;
import in.bachatsetu.backend.group.domain.model.PayoutMethod;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.time.LocalDate;

public final class GroupDomainFixtures {

    public static final Instant NOW = Instant.parse("2026-07-06T08:00:00Z");

    private GroupDomainFixtures() {
    }

    public static GroupRule monthlyRule(int maximumMembers) {
        return rule(ContributionFrequency.MONTHLY, maximumMembers);
    }

    public static GroupRule rule(ContributionFrequency frequency, int maximumMembers) {
        return new GroupRule(
                AggregateId.newId(),
                new ContributionSchedule(
                        Money.inr(100_000), frequency, LocalDate.of(2026, 8, 1), 12),
                new MemberCapacity(2, maximumMembers),
                PayoutMethod.RANDOM_DRAW,
                false);
    }

    public static SavingsGroup newGroup(int maximumMembers) {
        return newGroup(AggregateId.newId(), maximumMembers);
    }

    public static SavingsGroup newGroup(AggregateId ownerId, int maximumMembers) {
        return SavingsGroup.create(
                GroupId.newId(),
                AggregateId.newId(),
                new OwnerId(ownerId),
                new GroupCode("BS-TEST"),
                new GroupName("Bachat Circle"),
                new GroupDescription("Monthly community savings"),
                GroupType.BHISHI,
                monthlyRule(maximumMembers),
                new CreatedAt(NOW));
    }
}
