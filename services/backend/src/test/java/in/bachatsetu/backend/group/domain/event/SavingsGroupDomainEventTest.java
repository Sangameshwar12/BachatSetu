package in.bachatsetu.backend.group.domain.event;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.OwnerId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SavingsGroupDomainEventTest {

    @Test
    void carriesCreationAndMembershipFacts() {
        UUID eventId = UUID.randomUUID();
        AggregateId groupId = AggregateId.newId();
        AggregateId tenantId = AggregateId.newId();
        AggregateId ownerId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        SavingsGroupCreated created = new SavingsGroupCreated(
                eventId, groupId, tenantId, new OwnerId(ownerId), new GroupCode("BS-EVENT"), NOW);
        MemberJoined joined = new MemberJoined(UUID.randomUUID(), groupId, memberId, NOW.plusSeconds(1));
        MemberRemoved removed = new MemberRemoved(UUID.randomUUID(), groupId, memberId, NOW.plusSeconds(2));

        assertThat(created.eventId()).isEqualTo(eventId);
        assertThat(created.aggregateId()).isEqualTo(groupId);
        assertThat(created.tenantId()).isEqualTo(tenantId);
        assertThat(created.ownerId()).isEqualTo(new OwnerId(ownerId));
        assertThat(created.groupCode()).isEqualTo(new GroupCode("BS-EVENT"));
        assertThat(created.occurredAt()).isEqualTo(NOW);
        assertThat(created.eventType()).isEqualTo("SavingsGroupCreated");
        assertThat(joined.memberId()).isEqualTo(memberId);
        assertThat(removed.memberId()).isEqualTo(memberId);
    }

    @Test
    void carriesLifecycleFacts() {
        AggregateId groupId = AggregateId.newId();
        GroupActivated activated = new GroupActivated(UUID.randomUUID(), groupId, 1, NOW);
        GroupSuspended suspended = new GroupSuspended(UUID.randomUUID(), groupId, NOW.plusSeconds(1));
        GroupClosed closed = new GroupClosed(UUID.randomUUID(), groupId, NOW.plusSeconds(2));

        assertThat(activated.aggregateId()).isEqualTo(groupId);
        assertThat(activated.activeMemberCount()).isEqualTo(1);
        assertThat(suspended.aggregateId()).isEqualTo(groupId);
        assertThat(closed.aggregateId()).isEqualTo(groupId);
    }

    @Test
    void rejectsInvalidEventFacts() {
        AggregateId groupId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();

        assertThatThrownBy(() -> new GroupActivated(UUID.randomUUID(), groupId, 0, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MemberJoined(null, groupId, memberId, NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MemberRemoved(UUID.randomUUID(), null, memberId, NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GroupSuspended(UUID.randomUUID(), groupId, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GroupClosed(UUID.randomUUID(), null, NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SavingsGroupCreated(
                        UUID.randomUUID(), groupId, null, new OwnerId(memberId), new GroupCode("BS-EVENT"), NOW))
                .isInstanceOf(NullPointerException.class);
    }
}
