package in.bachatsetu.backend.group.domain.model;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.shared.domain.AggregateId;
import org.junit.jupiter.api.Test;

class GroupMemberTest {

    @Test
    void joinsAndRemovesImmutably() {
        AggregateId memberId = AggregateId.newId();
        GroupMember joined = GroupMember.join(memberId, new CreatedAt(NOW));

        GroupMember removed = joined.remove(new UpdatedAt(NOW.plusSeconds(1)));

        assertThat(joined.isActive()).isTrue();
        assertThat(joined.removedAt()).isNull();
        assertThat(removed.memberId()).isEqualTo(memberId);
        assertThat(removed.joinedAt()).isEqualTo(new CreatedAt(NOW));
        assertThat(removed.isActive()).isFalse();
        assertThatThrownBy(() -> removed.remove(new UpdatedAt(NOW.plusSeconds(2))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validatesMembershipTimeline() {
        AggregateId memberId = AggregateId.newId();

        assertThatThrownBy(() -> GroupMember.join(null, new CreatedAt(NOW)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> GroupMember.join(memberId, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> GroupMember.rehydrate(
                        memberId, new CreatedAt(NOW), new UpdatedAt(NOW.minusSeconds(1))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GroupMember.join(memberId, new CreatedAt(NOW)).remove(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void usesMemberIdentityForEquality() {
        AggregateId memberId = AggregateId.newId();
        GroupMember first = GroupMember.join(memberId, new CreatedAt(NOW));
        GroupMember second = GroupMember.rehydrate(
                memberId, new CreatedAt(NOW.minusSeconds(1)), new UpdatedAt(NOW));

        assertThat(first).isEqualTo(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(GroupMember.join(AggregateId.newId(), new CreatedAt(NOW)));
        assertThat(first).isNotEqualTo("member");
    }
}
