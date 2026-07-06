package in.bachatsetu.backend.group.domain.service;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.monthlyRule;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.rule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.group.domain.exception.DuplicateMemberException;
import in.bachatsetu.backend.group.domain.exception.GroupCapacityExceededException;
import in.bachatsetu.backend.group.domain.exception.InvalidGroupStateException;
import in.bachatsetu.backend.group.domain.model.ContributionAmount;
import in.bachatsetu.backend.group.domain.model.ContributionFrequency;
import in.bachatsetu.backend.group.domain.model.CreatedAt;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.GroupMember;
import in.bachatsetu.backend.group.domain.model.GroupRule;
import in.bachatsetu.backend.group.domain.model.MaximumMembers;
import in.bachatsetu.backend.group.domain.model.OwnerId;
import in.bachatsetu.backend.group.domain.model.UpdatedAt;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import org.junit.jupiter.api.Test;

class GroupDomainServiceTest {

    private final GroupValidationService validationService = new GroupValidationService();

    @Test
    void generatesStableNormalizedCode() {
        GroupId groupId = GroupId.from("12345678-90ab-cdef-1234-567890abcdef");
        GroupCodeGenerator generator = new GroupCodeGenerator();

        assertThat(generator.generate(groupId).value()).isEqualTo("BS-1234567890ABCDEF");
        assertThat(generator.generate(groupId)).isEqualTo(generator.generate(groupId));
        assertThatThrownBy(() -> generator.generate(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesMonthlyCreationRule() {
        AggregateId owner = AggregateId.newId();
        GroupRule monthlyRule = monthlyRule(10);

        validationService.validateCreation(
                new OwnerId(owner),
                new ContributionAmount(monthlyRule.contributionSchedule().contributionAmount()),
                new MaximumMembers(10),
                monthlyRule);

        GroupRule weeklyRule = rule(ContributionFrequency.WEEKLY, 10);
        assertThatThrownBy(() -> validationService.validateCreation(
                        new OwnerId(owner),
                        new ContributionAmount(weeklyRule.contributionSchedule().contributionAmount()),
                        new MaximumMembers(10),
                        weeklyRule))
                .isInstanceOf(InvalidGroupStateException.class);
    }

    @Test
    void validatesConsistentRuleProjection() {
        OwnerId owner = new OwnerId(AggregateId.newId());
        GroupRule rule = monthlyRule(10);

        assertThatThrownBy(() -> validationService.validateCreation(
                        owner, ContributionAmount.inPaise(200_000), new MaximumMembers(10), rule))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validationService.validateCreation(
                        owner,
                        new ContributionAmount(rule.contributionSchedule().contributionAmount()),
                        new MaximumMembers(9),
                        rule))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesRehydratedMemberships() {
        AggregateId owner = AggregateId.newId();
        GroupMember ownerMember = GroupMember.join(owner, new CreatedAt(NOW));
        GroupMember removedMember = GroupMember.rehydrate(
                AggregateId.newId(), new CreatedAt(NOW), new UpdatedAt(NOW.plusSeconds(1)));

        validationService.validateMemberships(
                new OwnerId(owner), new MaximumMembers(2), List.of(ownerMember, removedMember));

        assertThatThrownBy(() -> validationService.validateMemberships(
                        new OwnerId(owner), new MaximumMembers(2), List.of(ownerMember, ownerMember)))
                .isInstanceOf(DuplicateMemberException.class);
        assertThatThrownBy(() -> validationService.validateMemberships(
                        new OwnerId(owner), new MaximumMembers(2), List.of(removedMember)))
                .isInstanceOf(InvalidGroupStateException.class);
        assertThatThrownBy(() -> validationService.validateMemberships(
                        new OwnerId(owner),
                        new MaximumMembers(2),
                        List.of(
                                ownerMember,
                                GroupMember.join(AggregateId.newId(), new CreatedAt(NOW)),
                                GroupMember.join(AggregateId.newId(), new CreatedAt(NOW)))))
                .isInstanceOf(GroupCapacityExceededException.class);
    }

    @Test
    void rejectsNullValidationInputs() {
        GroupRule rule = monthlyRule(2);
        ContributionAmount amount = new ContributionAmount(rule.contributionSchedule().contributionAmount());
        MaximumMembers maximumMembers = new MaximumMembers(2);
        OwnerId owner = new OwnerId(AggregateId.newId());

        assertThatThrownBy(() -> validationService.validateCreation(null, amount, maximumMembers, rule))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> validationService.validateCreation(owner, null, maximumMembers, rule))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> validationService.validateCreation(owner, amount, null, rule))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> validationService.validateCreation(owner, amount, maximumMembers, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> validationService.validateMemberships(null, maximumMembers, List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> validationService.validateMemberships(owner, null, List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> validationService.validateMemberships(owner, maximumMembers, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> validationService.validateMemberships(
                        owner, maximumMembers, java.util.Arrays.asList((GroupMember) null)))
                .isInstanceOf(NullPointerException.class);
    }
}
