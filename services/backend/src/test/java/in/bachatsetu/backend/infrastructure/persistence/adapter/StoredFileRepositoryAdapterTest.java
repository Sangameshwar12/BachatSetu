package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.infrastructure.persistence.entity.storage.StoredFileJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.StoredFileJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.StoredFileSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import in.bachatsetu.backend.storage.domain.model.StoredFile;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StoredFileRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private StoredFileSpringDataRepository repository;
    private StoredFileJpaMapper mapper;
    private StoredFileRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(StoredFileSpringDataRepository.class);
        mapper = mock(StoredFileJpaMapper.class);
        adapter = new StoredFileRepositoryAdapter(repository, mapper);
    }

    @Test
    void findsByTenantAndId() {
        AggregateId tenantId = AggregateId.newId();
        StoredFile file = newFile(tenantId);
        StoredFileJpaEntity entity = mock(StoredFileJpaEntity.class);
        when(repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), file.id().value()))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(file);

        assertThat(adapter.findById(tenantId, file.id())).contains(file);
    }

    @Test
    void reportsNoMatch() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId fileId = AggregateId.newId();
        when(repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), fileId.value()))
                .thenReturn(Optional.empty());

        assertThat(adapter.findById(tenantId, fileId)).isEmpty();
    }

    @Test
    void savesNewAndUpdatedFiles() {
        StoredFile file = newFile(AggregateId.newId());
        StoredFileJpaEntity candidate = mock(StoredFileJpaEntity.class);
        when(repository.findById(file.id().value())).thenReturn(Optional.empty());
        when(mapper.toEntity(file)).thenReturn(candidate);

        adapter.save(file);

        verify(repository).save(candidate);
    }

    @Test
    void softDeletesByTenantAndId() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId fileId = AggregateId.newId();

        adapter.delete(tenantId, fileId);

        verify(repository).softDelete(
                org.mockito.ArgumentMatchers.eq(tenantId.value()), org.mockito.ArgumentMatchers.eq(fileId.value()),
                org.mockito.ArgumentMatchers.any());
    }

    private StoredFile newFile(AggregateId tenantId) {
        return StoredFile.upload(
                AggregateId.newId(), tenantId, StorageProvider.LOCAL, "/data/storage/tenant/file-1", "receipt.pdf",
                "application/pdf", 10L, "checksum", AggregateId.newId(), NOW);
    }
}
