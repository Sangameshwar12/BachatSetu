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

import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.GroupId;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        when(repository.findDistinctByTenantIdAndDeletedFalseOrderByCreatedAtAsc(tenantId.value()))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(group);

        assertThat(adapter.findById(tenantId, groupId)).contains(group);
        assertThat(adapter.findByGroupCode(tenantId, code)).contains(group);
        assertThat(adapter.findByCode(tenantId, code)).contains(group);
        assertThat(adapter.existsByGroupCode(tenantId, code)).isTrue();
        assertThat(adapter.findAll(tenantId)).containsExactly(group);
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
        assertThatThrownBy(() -> adapter.findAll(null)).isInstanceOf(NullPointerException.class);
    }
}
