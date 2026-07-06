package in.bachatsetu.backend.group.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.group.domain.exception.InvalidContributionAmountException;
import in.bachatsetu.backend.group.domain.exception.InvalidMaximumMembersException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class GroupValueObjectTest {

    @Test
    void normalizesTextAndCodes() {
        assertThat(new GroupName("  My Group  ").value()).isEqualTo("My Group");
        assertThat(new GroupDescription("  Trusted neighbours  ").value()).isEqualTo("Trusted neighbours");
        assertThat(GroupDescription.empty().value()).isEmpty();
        assertThat(new GroupCode(" bs-local ").value()).isEqualTo("BS-LOCAL");
    }

    @Test
    void validatesGroupNameBoundaries() {
        assertThat(new GroupName("abc").value()).hasSize(3);
        assertThat(new GroupName("a".repeat(100)).value()).hasSize(100);
        assertThatThrownBy(() -> new GroupName("ab")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GroupName("a".repeat(101))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GroupName(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesDescriptionAndCode() {
        assertThatThrownBy(() -> new GroupDescription(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GroupCode("bad code")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GroupCode(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesContributionRangeAndCurrency() {
        assertThat(ContributionAmount.inPaise(1).value()).isEqualTo(Money.inr(1));
        assertThat(ContributionAmount.inPaise(ContributionAmount.MAXIMUM_PAISE).value())
                .isEqualTo(Money.inr(ContributionAmount.MAXIMUM_PAISE));
        assertThatThrownBy(() -> ContributionAmount.inPaise(0))
                .isInstanceOf(InvalidContributionAmountException.class);
        assertThatThrownBy(() -> ContributionAmount.inPaise(-1))
                .isInstanceOf(InvalidContributionAmountException.class);
        assertThatThrownBy(() -> ContributionAmount.inPaise(ContributionAmount.MAXIMUM_PAISE + 1))
                .isInstanceOf(InvalidContributionAmountException.class);
        assertThatThrownBy(() -> new ContributionAmount(
                        new Money(100, Currency.getInstance("USD"))))
                .isInstanceOf(InvalidContributionAmountException.class);
        assertThatThrownBy(() -> new ContributionAmount(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesMaximumMembersAndMemberCount() {
        MaximumMembers minimum = new MaximumMembers(2);
        MaximumMembers maximum = new MaximumMembers(500);

        assertThat(minimum.accommodates(new MemberCount(2))).isTrue();
        assertThat(minimum.accommodates(new MemberCount(3))).isFalse();
        assertThat(maximum.value()).isEqualTo(500);
        assertThatThrownBy(() -> new MaximumMembers(1)).isInstanceOf(InvalidMaximumMembersException.class);
        assertThatThrownBy(() -> new MaximumMembers(501)).isInstanceOf(InvalidMaximumMembersException.class);
        assertThatThrownBy(() -> new MemberCount(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void safelyChangesMemberCount() {
        assertThat(new MemberCount(1).increment()).isEqualTo(new MemberCount(2));
        assertThat(new MemberCount(1).decrement()).isEqualTo(new MemberCount(0));
        assertThatThrownBy(() -> new MemberCount(0).decrement()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new MemberCount(Integer.MAX_VALUE).increment())
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void validatesStrongIdentifiersAndTimestamps() {
        AggregateId aggregateId = AggregateId.newId();
        GroupId groupId = new GroupId(aggregateId);

        assertThat(groupId.value()).isEqualTo(aggregateId);
        assertThat(GroupId.from(aggregateId.toString())).isEqualTo(groupId);
        assertThat(GroupId.newId()).isNotEqualTo(groupId);
        assertThat(new OwnerId(aggregateId).value()).isEqualTo(aggregateId);
        assertThat(new CreatedAt(Instant.EPOCH).value()).isEqualTo(Instant.EPOCH);
        assertThat(new UpdatedAt(Instant.EPOCH).value()).isEqualTo(Instant.EPOCH);
        assertThatThrownBy(() -> new GroupId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OwnerId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreatedAt(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdatedAt(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void definesOnlySupportedLifecycleStates() {
        assertThat(GroupStatus.values())
                .containsExactly(GroupStatus.ACTIVE, GroupStatus.INACTIVE, GroupStatus.SUSPENDED, GroupStatus.CLOSED);
    }
}
