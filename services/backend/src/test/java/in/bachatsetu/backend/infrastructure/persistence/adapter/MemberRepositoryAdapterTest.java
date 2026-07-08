package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupMemberJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.exception.PersistenceMappingException;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.mapper.MemberJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.MemberSpringDataRepository;
import in.bachatsetu.backend.member.domain.model.GroupParticipation;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.model.MemberStatus;
import in.bachatsetu.backend.member.domain.model.ParticipationStatus;
import in.bachatsetu.backend.member.domain.port.MemberPage;
import in.bachatsetu.backend.member.domain.port.MemberPageRequest;
import in.bachatsetu.backend.member.domain.port.MemberSortField;
import in.bachatsetu.backend.member.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class MemberRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-06T08:00:00Z");

    private MemberSpringDataRepository repository;
    private MemberJpaMapper mapper;
    private JpaReferenceProvider references;
    private MemberRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(MemberSpringDataRepository.class);
        mapper = mock(MemberJpaMapper.class);
        references = mock(JpaReferenceProvider.class);
        adapter = new MemberRepositoryAdapter(repository, mapper, references);
    }

    @Test
    void findsByLegacyIdentifier() {
        AggregateId memberId = AggregateId.newId();
        GroupMemberJpaEntity entity = mock(GroupMemberJpaEntity.class);
        MemberProfile profile = memberWithOneParticipation(memberId, AggregateId.newId());
        when(repository.findByIdAndDeletedFalse(memberId.value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(profile);

        assertThat(adapter.findById(memberId)).contains(profile);
    }

    @Test
    void findsByTenantScopedIdentifier() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        GroupMemberJpaEntity entity = mock(GroupMemberJpaEntity.class);
        MemberProfile profile = memberWithOneParticipation(memberId, AggregateId.newId());
        when(repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), memberId.value()))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(profile);

        assertThat(adapter.findById(tenantId, memberId)).contains(profile);
    }

    @Test
    void reportsNoTenantScopedMatch() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        when(repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), memberId.value()))
                .thenReturn(Optional.empty());

        assertThat(adapter.findById(tenantId, memberId)).isEmpty();
    }

    @Test
    void findsByUserIdAssemblingAllParticipationRows() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId userId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        GroupMemberJpaEntity firstRow = mock(GroupMemberJpaEntity.class);
        GroupMemberJpaEntity secondRow = mock(GroupMemberJpaEntity.class);
        MemberProfile firstProfile = memberWithOneParticipation(memberId, AggregateId.newId());
        MemberProfile secondProfile = memberWithOneParticipation(memberId, AggregateId.newId());
        when(repository.findAllByTenantIdAndUser_IdAndDeletedFalseOrderByJoinedAtAsc(tenantId.value(), userId.value()))
                .thenReturn(List.of(firstRow, secondRow));
        when(mapper.toDomain(firstRow)).thenReturn(firstProfile);
        when(mapper.toDomain(secondRow)).thenReturn(secondProfile);

        MemberProfile assembled = adapter.findByUserId(tenantId, userId).orElseThrow();

        assertThat(assembled.id()).isEqualTo(firstProfile.id());
        assertThat(assembled.participations()).hasSize(2);
    }

    @Test
    void reportsNoMatchingUser() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId userId = AggregateId.newId();
        when(repository.findAllByTenantIdAndUser_IdAndDeletedFalseOrderByJoinedAtAsc(tenantId.value(), userId.value()))
                .thenReturn(List.of());

        assertThat(adapter.findByUserId(tenantId, userId)).isEmpty();
    }

    @Test
    void findsByUserIdAcrossTenantsAssemblingAllParticipationRows() {
        AggregateId userId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        GroupMemberJpaEntity firstRow = mock(GroupMemberJpaEntity.class);
        GroupMemberJpaEntity secondRow = mock(GroupMemberJpaEntity.class);
        MemberProfile firstProfile = memberWithOneParticipation(memberId, AggregateId.newId());
        MemberProfile secondProfile = memberWithOneParticipation(memberId, AggregateId.newId());
        when(repository.findAllByUser_IdAndDeletedFalseOrderByJoinedAtAsc(userId.value()))
                .thenReturn(List.of(firstRow, secondRow));
        when(mapper.toDomain(firstRow)).thenReturn(firstProfile);
        when(mapper.toDomain(secondRow)).thenReturn(secondProfile);

        MemberProfile assembled = adapter.findByUserId(userId).orElseThrow();

        assertThat(assembled.id()).isEqualTo(firstProfile.id());
        assertThat(assembled.participations()).hasSize(2);
    }

    @Test
    void reportsNoMatchingUserAcrossTenants() {
        AggregateId userId = AggregateId.newId();
        when(repository.findAllByUser_IdAndDeletedFalseOrderByJoinedAtAsc(userId.value())).thenReturn(List.of());

        assertThat(adapter.findByUserId(userId)).isEmpty();
    }

    @Test
    void findsByMemberNumber() {
        AggregateId tenantId = AggregateId.newId();
        MemberNumber memberNumber = new MemberNumber("MB-1A2B3C4D5E6F7A8B");
        GroupMemberJpaEntity entity = mock(GroupMemberJpaEntity.class);
        MemberProfile profile = memberWithOneParticipation(AggregateId.newId(), AggregateId.newId());
        when(repository.findFirstByTenantIdAndMemberNumberAndDeletedFalseOrderByJoinedAtAsc(
                        tenantId.value(), memberNumber.value()))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(profile);

        assertThat(adapter.findByMemberNumber(tenantId, memberNumber)).contains(profile);
    }

    @Test
    void findsPageAssemblingEachRepresentativeRow() {
        AggregateId tenantId = AggregateId.newId();
        UUID userId = UUID.randomUUID();
        GroupMemberJpaEntity representativeRow = mock(GroupMemberJpaEntity.class);
        UserJpaEntity user = mock(UserJpaEntity.class);
        when(user.getId()).thenReturn(userId);
        when(representativeRow.getTenantId()).thenReturn(tenantId.value());
        when(representativeRow.getUser()).thenReturn(user);
        GroupMemberJpaEntity fullRow = mock(GroupMemberJpaEntity.class);
        MemberProfile profile = memberWithOneParticipation(AggregateId.newId(), AggregateId.newId());
        when(repository.findRepresentativeRowsByTenantId(
                        org.mockito.ArgumentMatchers.eq(tenantId.value()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(representativeRow), PageRequest.of(0, 20), 1));
        when(repository.findAllByTenantIdAndUser_IdAndDeletedFalseOrderByJoinedAtAsc(tenantId.value(), userId))
                .thenReturn(List.of(fullRow));
        when(mapper.toDomain(fullRow)).thenReturn(profile);
        MemberPageRequest pageRequest = new MemberPageRequest(0, 20, MemberSortField.MEMBER_NUMBER, SortDirection.ASC);

        MemberPage<MemberProfile> page = adapter.findPage(tenantId, pageRequest);

        assertThat(page.content()).singleElement()
                .satisfies(assembled -> assertThat(assembled.id()).isEqualTo(profile.id()));
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void appliesRequestedSortWhenBuildingThePageable() {
        AggregateId tenantId = AggregateId.newId();
        Page<GroupMemberJpaEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(1, 5), 6);
        when(repository.findRepresentativeRowsByTenantId(
                        org.mockito.ArgumentMatchers.eq(tenantId.value()), any(Pageable.class)))
                .thenReturn(emptyPage);
        MemberPageRequest pageRequest = new MemberPageRequest(1, 5, MemberSortField.CREATED_AT, SortDirection.DESC);

        MemberPage<MemberProfile> page = adapter.findPage(tenantId, pageRequest);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(6);
    }

    @Test
    void rejectsSavingAMemberWithoutAnyParticipation() {
        MemberProfile withoutParticipation = MemberProfile.create(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                new MemberNumber("MB-NOPARTICIPATE"), AggregateId.newId(), NOW);

        assertThatThrownBy(() -> adapter.save(withoutParticipation))
                .isInstanceOf(PersistenceMappingException.class)
                .hasMessageContaining("at least one group participation");
    }

    @Test
    void savesEveryParticipationRowForAMember() {
        AggregateId memberId = AggregateId.newId();
        AggregateId groupId = AggregateId.newId();
        MemberProfile member = memberWithOneParticipation(memberId, groupId);
        GroupParticipation participation = member.participations().get(0);
        GroupMemberJpaEntity candidate = mock(GroupMemberJpaEntity.class);
        when(repository.findById(participation.id().value())).thenReturn(Optional.empty());
        when(mapper.toEntity(member, participation, references)).thenReturn(candidate);

        adapter.save(member);

        verify(repository).save(candidate);
    }

    private MemberProfile memberWithOneParticipation(AggregateId memberId, AggregateId groupId) {
        AggregateId tenantId = AggregateId.newId();
        AggregateId userId = AggregateId.newId();
        GroupParticipation participation = new GroupParticipation(
                AggregateId.newId(), groupId, GroupRole.MEMBER, NOW, ParticipationStatus.ACTIVE, null);
        return new MemberProfile(
                memberId,
                tenantId,
                userId,
                new MemberNumber("MB-1A2B3C4D5E6F7A8B"),
                MemberStatus.ACTIVE,
                List.of(participation),
                List.of(),
                AuditInfo.createdBy(userId, NOW),
                0);
    }
}
