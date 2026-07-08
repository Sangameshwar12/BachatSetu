package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.audit.domain.model.AuditEntry;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.audit.domain.port.AuditPage;
import in.bachatsetu.backend.audit.domain.port.AuditSearchCriteria;
import in.bachatsetu.backend.audit.domain.port.AuditSortField;
import in.bachatsetu.backend.audit.domain.port.SortDirection;
import in.bachatsetu.backend.infrastructure.persistence.entity.audit.AuditEntryJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.AuditEntryJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.AuditEntrySpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class AuditEntryRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private AuditEntrySpringDataRepository repository;
    private AuditEntryJpaMapper mapper;
    private AuditEntryRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(AuditEntrySpringDataRepository.class);
        mapper = mock(AuditEntryJpaMapper.class);
        adapter = new AuditEntryRepositoryAdapter(repository, mapper);
    }

    @Test
    void findsAnEntryScopedToItsOwnTenant() {
        AggregateId tenantId = AggregateId.newId();
        AuditEntry entry = newEntry(tenantId);
        AuditEntryJpaEntity entity = mock(AuditEntryJpaEntity.class);
        when(repository.findByIdAndDeletedFalse(entry.id().value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(entry);

        assertThat(adapter.findById(tenantId, entry.id())).contains(entry);
    }

    @Test
    void hidesAnEntryBelongingToAnotherTenant() {
        AggregateId tenantId = AggregateId.newId();
        AuditEntry entry = newEntry(AggregateId.newId());
        AuditEntryJpaEntity entity = mock(AuditEntryJpaEntity.class);
        when(repository.findByIdAndDeletedFalse(entry.id().value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(entry);

        assertThat(adapter.findById(tenantId, entry.id())).isEmpty();
    }

    @Test
    void reportsNoMatchWhenTheEntryDoesNotExist() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId auditId = AggregateId.newId();
        when(repository.findByIdAndDeletedFalse(auditId.value())).thenReturn(Optional.empty());

        assertThat(adapter.findById(tenantId, auditId)).isEmpty();
    }

    @Test
    void searchesAndMapsAPageOfEntries() {
        AggregateId tenantId = AggregateId.newId();
        AuditEntry entry = newEntry(tenantId);
        AuditEntryJpaEntity entity = mock(AuditEntryJpaEntity.class);
        Page<AuditEntryJpaEntity> jpaPage = new PageImpl<>(List.of(entity));
        when(repository.search(
                        org.mockito.ArgumentMatchers.eq(tenantId.value()), any(), any(), any(), any(), any(),
                        any(Pageable.class)))
                .thenReturn(jpaPage);
        when(mapper.toDomain(entity)).thenReturn(entry);
        AuditSearchCriteria criteria = new AuditSearchCriteria(
                tenantId, null, null, null, null, null, 0, 20, AuditSortField.CREATED_AT, SortDirection.DESC);

        AuditPage<AuditEntry> result = adapter.search(criteria);

        assertThat(result.content()).containsExactly(entry);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void savesANewEntry() {
        AuditEntry entry = newEntry(AggregateId.newId());
        AuditEntryJpaEntity candidate = mock(AuditEntryJpaEntity.class);
        when(repository.findById(entry.id().value())).thenReturn(Optional.empty());
        when(mapper.toEntity(entry)).thenReturn(candidate);

        adapter.save(entry);

        verify(repository).save(candidate);
    }

    private AuditEntry newEntry(AggregateId tenantId) {
        return AuditEntry.record(
                AggregateId.newId(), tenantId, AggregateId.newId(), AuditEventType.LOGIN, "auth", null, null,
                "LOGIN", null, null, null, null, NOW);
    }
}
