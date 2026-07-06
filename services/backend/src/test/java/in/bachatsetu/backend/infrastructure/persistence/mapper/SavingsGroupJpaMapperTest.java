package in.bachatsetu.backend.infrastructure.persistence.mapper;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.monthlyRule;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.newGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.group.domain.model.GroupType;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupMemberJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.MembershipHistoryEventType;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.MembershipHistoryJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.SavingsGroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.member.domain.model.ParticipationStatus;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class SavingsGroupJpaMapperTest {

    private SavingsGroupJpaMapper mapper;
    private JpaReferenceProvider references;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(SavingsGroupJpaMapper.class);
        references = mock(JpaReferenceProvider.class);
        when(references.user(any(AggregateId.class))).thenAnswer(invocation -> {
            AggregateId id = invocation.getArgument(0);
            UserJpaEntity user = mock(UserJpaEntity.class);
            when(user.getId()).thenReturn(id.value());
            return user;
        });
    }

    @Test
    void mapsAggregateToCompleteJpaGraphAndMembershipHistory() {
        SavingsGroup group = newGroup(5);
        AggregateId memberId = AggregateId.newId();
        group.activate(group.organizerId(), NOW.plusSeconds(1));
        group.joinMember(memberId, group.organizerId(), NOW.plusSeconds(2));
        group.removeMember(memberId, group.organizerId(), NOW.plusSeconds(3));

        SavingsGroupJpaEntity entity = mapper.toEntity(group, references);

        assertThat(entity.getId()).isEqualTo(group.id().value());
        assertThat(entity.getTenantId()).isEqualTo(group.tenantId().value());
        assertThat(entity.getOrganizer().getId()).isEqualTo(group.organizerId().value());
        assertThat(entity.getCode()).isEqualTo(group.code().value());
        assertThat(entity.getName()).isEqualTo(group.name().value());
        assertThat(entity.getDescription()).isEqualTo(group.description().value());
        assertThat(entity.getStatus()).isEqualTo(GroupStatus.ACTIVE);
        assertThat(entity.getMaximumMembers()).isEqualTo(5);
        assertThat(entity.getMembers()).hasSize(2);
        assertThat(entity.getMembers()).filteredOn(member -> member.getUser().getId().equals(memberId.value()))
                .singleElement()
                .satisfies(member -> {
                    assertThat(member.getExitedAt()).isEqualTo(NOW.plusSeconds(3));
                    assertThat(member.getHistory()).hasSize(2);
                });
        assertThat(entity.getMembers()).extracting(GroupMemberJpaEntity::getId).doesNotHaveDuplicates();
    }

    @Test
    void rehydratesAggregateWithoutEvents() {
        UUID groupId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        SavingsGroupJpaEntity entity = mock(SavingsGroupJpaEntity.class);
        UserJpaEntity owner = mock(UserJpaEntity.class);
        GroupMemberJpaEntity ownerMembership = mock(GroupMemberJpaEntity.class);
        stubAudit(entity, groupId, ownerId, 4);
        when(entity.getTenantId()).thenReturn(tenantId);
        when(entity.getOrganizer()).thenReturn(owner);
        when(owner.getId()).thenReturn(ownerId);
        when(entity.getCode()).thenReturn("BS-RESTORED");
        when(entity.getName()).thenReturn("Restored Group");
        when(entity.getDescription()).thenReturn("Persistence reconstruction");
        when(entity.getType()).thenReturn(GroupType.BHISHI);
        when(entity.getStatus()).thenReturn(GroupStatus.SUSPENDED);
        when(entity.getContributionAmountPaise()).thenReturn(100_000L);
        when(entity.getCurrencyCode()).thenReturn("INR");
        when(entity.getFrequency()).thenReturn(monthlyRule(5).contributionSchedule().frequency());
        when(entity.getStartDate()).thenReturn(monthlyRule(5).contributionSchedule().startDate());
        when(entity.getDurationCycles()).thenReturn(12);
        when(entity.getMinimumMembers()).thenReturn(2);
        when(entity.getMaximumMembers()).thenReturn(5);
        when(entity.getPayoutMethod()).thenReturn(monthlyRule(5).payoutMethod());
        when(ownerMembership.getUser()).thenReturn(owner);
        when(ownerMembership.getJoinedAt()).thenReturn(NOW);
        when(ownerMembership.getExitedAt()).thenReturn(null);
        when(entity.getMembers()).thenReturn(List.of(ownerMembership));

        SavingsGroup restored = mapper.toDomain(entity);

        assertThat(restored.id().value()).isEqualTo(groupId);
        assertThat(restored.tenantId().value()).isEqualTo(tenantId);
        assertThat(restored.description().value()).isEqualTo("Persistence reconstruction");
        assertThat(restored.status()).isEqualTo(GroupStatus.SUSPENDED);
        assertThat(restored.version()).isEqualTo(4);
        assertThat(restored.domainEvents()).isEmpty();
    }

    @Test
    void updatesExistingEntityWithoutChangingItsIdentity() {
        SavingsGroup group = newGroup(5);
        SavingsGroupJpaEntity existing = mapper.toEntity(group, references);
        UUID legacyMembershipId = UUID.randomUUID();
        UUID legacyHistoryId = UUID.randomUUID();
        GroupMemberJpaEntity legacyOwner = new GroupMemberJpaEntity(
                legacyMembershipId,
                group.tenantId().value(),
                existing,
                existing.getOrganizer(),
                group.organizerId().toString().replace("-", ""),
                GroupRole.ORGANIZER,
                ParticipationStatus.ACTIVE,
                NOW,
                null);
        legacyOwner.replaceHistory(List.of(new MembershipHistoryJpaEntity(
                legacyHistoryId,
                group.tenantId().value(),
                existing,
                legacyOwner,
                group.organizerId().value(),
                MembershipHistoryEventType.JOINED,
                NOW)));
        existing.synchronizeMembers(List.of(legacyOwner));
        group.activate(group.organizerId(), NOW.plusSeconds(1));

        SavingsGroupJpaEntity updated = mapper.updateEntity(group, existing, references);

        assertThat(updated).isSameAs(existing);
        assertThat(updated.getId()).isEqualTo(group.id().value());
        assertThat(updated.getStatus()).isEqualTo(GroupStatus.ACTIVE);
        assertThat(updated.getMembers()).hasSize(1);
        assertThat(updated.getMembers().getFirst().getId()).isEqualTo(legacyMembershipId);
        assertThat(updated.getMembers().getFirst().getHistory()).extracting(BaseJpaEntity::getId)
                .containsExactly(legacyHistoryId);
    }

    @Test
    void handlesNullMappingBoundaries() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toEntity(null, references)).isNull();
        assertThatThrownBy(() -> mapper.updateEntity(null, mock(SavingsGroupJpaEntity.class), references))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.updateEntity(newGroup(5), null, references))
                .isInstanceOf(NullPointerException.class);
    }

    private void stubAudit(BaseJpaEntity entity, UUID id, UUID actorId, long version) {
        when(entity.getId()).thenReturn(id);
        when(entity.getCreatedAt()).thenReturn(NOW);
        when(entity.getUpdatedAt()).thenReturn(NOW.plusSeconds(1));
        when(entity.getCreatedBy()).thenReturn(actorId);
        when(entity.getUpdatedBy()).thenReturn(actorId);
        when(entity.getVersion()).thenReturn(version);
    }
}
