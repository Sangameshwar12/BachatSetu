package in.bachatsetu.backend.group.application.mapper;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.newGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.group.application.query.GroupMemberResult;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;
import in.bachatsetu.backend.group.application.query.SavingsGroupSummary;
import in.bachatsetu.backend.group.domain.model.GroupMember;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SavingsGroupApplicationMapperTest {

    private final SavingsGroupApplicationMapper mapper = new SavingsGroupApplicationMapper();

    @Test
    void mapsCompleteAggregateAndMembershipHistory() {
        SavingsGroup group = newGroup(5);
        AggregateId memberId = AggregateId.newId();
        group.activate(group.organizerId(), NOW.plusSeconds(1));
        group.joinMember(memberId, group.organizerId(), NOW.plusSeconds(2));
        group.removeMember(memberId, group.organizerId(), NOW.plusSeconds(3));

        SavingsGroupResult result = mapper.toResult(group);

        assertThat(result.groupId()).isEqualTo(group.id().value());
        assertThat(result.tenantId()).isEqualTo(group.tenantId().value());
        assertThat(result.ownerId()).isEqualTo(group.ownerId().value().value());
        assertThat(result.groupCode()).isEqualTo(group.code().value());
        assertThat(result.name()).isEqualTo(group.name().value());
        assertThat(result.description()).isEqualTo(group.description().value());
        assertThat(result.type()).isEqualTo(group.type().name());
        assertThat(result.status()).isEqualTo(group.status().name());
        assertThat(result.contributionAmountPaise()).isEqualTo(100_000);
        assertThat(result.currencyCode()).isEqualTo("INR");
        assertThat(result.maximumMembers()).isEqualTo(5);
        assertThat(result.activeMemberCount()).isEqualTo(1);
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.updatedAt()).isEqualTo(NOW.plusSeconds(3));
        assertThat(result.version()).isEqualTo(3);
        assertThat(result.members()).hasSize(2);
        assertThat(result.members()).filteredOn(member -> member.memberId().equals(memberId.value()))
                .singleElement()
                .satisfies(member -> {
                    assertThat(member.joinedAt()).isEqualTo(NOW.plusSeconds(2));
                    assertThat(member.removedAt()).isEqualTo(NOW.plusSeconds(3));
                    assertThat(member.active()).isFalse();
                });
        assertThatThrownBy(() -> result.members().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mapsSummaryAndActiveMember() {
        SavingsGroup group = newGroup(7);
        GroupMember owner = group.members().getFirst();

        SavingsGroupSummary summary = mapper.toSummary(group);
        GroupMemberResult member = mapper.toMemberResult(owner);

        assertThat(summary.groupId()).isEqualTo(group.id().value());
        assertThat(summary.groupCode()).isEqualTo(group.code().value());
        assertThat(summary.name()).isEqualTo(group.name().value());
        assertThat(summary.status()).isEqualTo(group.status().name());
        assertThat(summary.contributionAmountPaise()).isEqualTo(100_000);
        assertThat(summary.currencyCode()).isEqualTo("INR");
        assertThat(summary.maximumMembers()).isEqualTo(7);
        assertThat(summary.activeMemberCount()).isEqualTo(1);
        assertThat(member.memberId()).isEqualTo(owner.memberId().value());
        assertThat(member.removedAt()).isNull();
        assertThat(member.active()).isTrue();
    }

    @Test
    void rejectsNullMappingInputsAndInconsistentMembershipResults() {
        assertThatThrownBy(() -> mapper.toResult(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toSummary(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toMemberResult(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GroupMemberResult(UUID.randomUUID(), NOW, NOW, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GroupMemberResult(UUID.randomUUID(), NOW, null, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GroupMemberResult(null, NOW, null, true))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GroupMemberResult(UUID.randomUUID(), null, null, true))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void queryResultDefensivelyCopiesMembers() {
        UUID id = UUID.randomUUID();
        GroupMemberResult member = new GroupMemberResult(UUID.randomUUID(), NOW, null, true);
        List<GroupMemberResult> source = new ArrayList<>(List.of(member));

        SavingsGroupResult result = new SavingsGroupResult(
                id,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BS-QUERY",
                "Query Group",
                "Description",
                "BHISHI",
                "ACTIVE",
                100,
                "INR",
                2,
                1,
                NOW,
                NOW,
                0,
                source,
                "Query Organizer");
        source.clear();

        assertThat(result.members()).containsExactly(member);
    }
}
