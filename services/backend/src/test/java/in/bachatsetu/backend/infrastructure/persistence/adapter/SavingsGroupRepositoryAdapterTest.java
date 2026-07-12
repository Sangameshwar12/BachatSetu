package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.newGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.group.application.port.GroupPage;
import in.bachatsetu.backend.group.application.port.GroupPageRequest;
import in.bachatsetu.backend.group.application.port.GroupSortField;
import in.bachatsetu.backend.group.application.port.SortDirection;
import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.SavingsGroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.exception.PersistenceConflictException;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.mapper.SavingsGroupJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SavingsGroupSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class SavingsGroupRepositoryAdapterTest {

    private SavingsGroupSpringDataRepository repository;
    private SavingsGroupJpaMapper mapper;
    private JpaReferenceProvider references;
    private SavingsGroupRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(SavingsGroupSpringDataRepository.class);
        mapper = mock(SavingsGroupJpaMapper.class);
        references = mock(JpaReferenceProvider.class);
        adapter = new SavingsGroupRepositoryAdapter(repository, mapper, references);
    }

    @Test
    void savesNewAndUpdatedAggregates() {
        SavingsGroup newGroup = newGroup(5);
        SavingsGroupJpaEntity newEntity = mock(SavingsGroupJpaEntity.class);
        when(mapper.toEntity(newGroup, references)).thenReturn(newEntity);

        adapter.save(newGroup);

        verify(repository).save(newEntity);

        SavingsGroup updatedGroup = newGroup(5);
        updatedGroup.activate(updatedGroup.organizerId(), NOW.plusSeconds(1));
        SavingsGroupJpaEntity existing = mock(SavingsGroupJpaEntity.class);
        when(existing.getVersion()).thenReturn(0L);
        when(repository.findById(updatedGroup.id().value())).thenReturn(Optional.of(existing));
        when(mapper.updateEntity(updatedGroup, existing, references)).thenReturn(existing);

        adapter.save(updatedGroup);

        verify(mapper).updateEntity(updatedGroup, existing, references);
        verify(repository).save(existing);
    }

    @Test
    void rejectsStaleAggregateVersion() {
        SavingsGroup stale = newGroup(5);
        stale.activate(stale.organizerId(), NOW.plusSeconds(1));
        SavingsGroupJpaEntity existing = mock(SavingsGroupJpaEntity.class);
        when(existing.getVersion()).thenReturn(1L);
        when(repository.findById(stale.id().value())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> adapter.save(stale))
                .isInstanceOf(PersistenceConflictException.class)
                .hasMessageContaining("stale");
        verify(mapper, never()).updateEntity(any(), any(), any());
    }

    @Test
    void supportsAllTenantScopedReads() {
        SavingsGroup group = newGroup(5);
        SavingsGroupJpaEntity entity = mock(SavingsGroupJpaEntity.class);
        AggregateId tenantId = group.tenantId();
        GroupId groupId = group.groupId();
        GroupCode code = group.code();
        when(repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), groupId.value().value()))
                .thenReturn(Optional.of(entity));
        when(repository.findByTenantIdAndCodeAndDeletedFalse(tenantId.value(), code.value()))
                .thenReturn(Optional.of(entity));
        when(repository.existsByTenantIdAndCodeAndDeletedFalse(tenantId.value(), code.value()))
                .thenReturn(true);
        GroupPageRequest pageRequest = new GroupPageRequest(0, 20, GroupSortField.CREATED_AT, SortDirection.ASC, null);
        when(entity.getId()).thenReturn(groupId.value().value());
        when(repository.findPageIdsByTenantIdAndOptionalStatus(
                        org.mockito.ArgumentMatchers.eq(tenantId.value()),
                        org.mockito.ArgumentMatchers.isNull(),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(groupId.value().value()), PageRequest.of(0, 20), 1));
        when(repository.findByIdInAndDeletedFalse(List.of(groupId.value().value())))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(group);

        assertThat(adapter.findById(tenantId, groupId)).contains(group);
        assertThat(adapter.findByGroupCode(tenantId, code)).contains(group);
        assertThat(adapter.findByCode(tenantId, code)).contains(group);
        assertThat(adapter.existsByGroupCode(tenantId, code)).isTrue();
        GroupPage<SavingsGroup> page = adapter.findPage(tenantId, pageRequest);
        assertThat(page.content()).containsExactly(group);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void appliesRequestedSortAndStatusFilterWhenBuildingThePageable() {
        SavingsGroupJpaEntity entity = mock(SavingsGroupJpaEntity.class);
        SavingsGroup group = newGroup(5);
        AggregateId tenantId = group.tenantId();
        GroupPageRequest pageRequest = new GroupPageRequest(
                1, 5, GroupSortField.NAME, SortDirection.DESC, GroupStatus.ACTIVE);
        when(entity.getId()).thenReturn(group.groupId().value().value());
        when(repository.findPageIdsByTenantIdAndOptionalStatus(
                        org.mockito.ArgumentMatchers.eq(tenantId.value()),
                        org.mockito.ArgumentMatchers.eq(GroupStatus.ACTIVE),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(group.groupId().value().value()), PageRequest.of(1, 5), 6));
        when(repository.findByIdInAndDeletedFalse(List.of(group.groupId().value().value())))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(group);

        GroupPage<SavingsGroup> page = adapter.findPage(tenantId, pageRequest);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(6);
    }

    @Test
    void preservesRequestedSortOrderWhenReassemblingPageFromIds() {
        SavingsGroup groupA = newGroup(5);
        SavingsGroup groupB = newGroup(5);
        SavingsGroupJpaEntity entityA = mock(SavingsGroupJpaEntity.class);
        SavingsGroupJpaEntity entityB = mock(SavingsGroupJpaEntity.class);
        AggregateId tenantId = groupA.tenantId();
        UUID idA = groupA.groupId().value().value();
        UUID idB = groupB.groupId().value().value();
        when(entityA.getId()).thenReturn(idA);
        when(entityB.getId()).thenReturn(idB);
        GroupPageRequest pageRequest = new GroupPageRequest(0, 20, GroupSortField.CREATED_AT, SortDirection.ASC, null);
        when(repository.findPageIdsByTenantIdAndOptionalStatus(
                        org.mockito.ArgumentMatchers.eq(tenantId.value()),
                        org.mockito.ArgumentMatchers.isNull(),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(idA, idB), PageRequest.of(0, 20), 2));
        // Intentionally returned out of requested order — findByIdIn does not guarantee row order.
        when(repository.findByIdInAndDeletedFalse(List.of(idA, idB))).thenReturn(List.of(entityB, entityA));
        when(mapper.toDomain(entityA)).thenReturn(groupA);
        when(mapper.toDomain(entityB)).thenReturn(groupB);

        GroupPage<SavingsGroup> page = adapter.findPage(tenantId, pageRequest);

        assertThat(page.content()).containsExactly(groupA, groupB);
    }

    @Test
    void supportsLegacyLookupAndSoftDelete() {
        SavingsGroup group = newGroup(5);
        SavingsGroupJpaEntity entity = mock(SavingsGroupJpaEntity.class);
        when(repository.findByIdAndDeletedFalse(group.id().value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(group);

        assertThat(adapter.findById(group.id())).contains(group);

        adapter.delete(group.tenantId(), group.groupId());

        verify(repository).softDelete(
                org.mockito.ArgumentMatchers.eq(group.tenantId().value()),
                org.mockito.ArgumentMatchers.eq(group.id().value()),
                any(Instant.class));
    }

    @Test
    void validatesRequiredDependenciesAndInputs() {
        assertThatThrownBy(() -> new SavingsGroupRepositoryAdapter(null, mapper, references))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SavingsGroupRepositoryAdapter(repository, null, references))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SavingsGroupRepositoryAdapter(repository, mapper, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> adapter.save(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> adapter.findById(null, GroupId.newId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> adapter.findByGroupCode(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
        GroupPageRequest pageRequest = new GroupPageRequest(0, 20, GroupSortField.CREATED_AT, SortDirection.ASC, null);
        assertThatThrownBy(() -> adapter.findPage(null, pageRequest)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> adapter.findPage(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
