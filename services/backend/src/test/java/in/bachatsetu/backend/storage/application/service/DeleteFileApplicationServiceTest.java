package in.bachatsetu.backend.storage.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.application.exception.StoredFileNotFoundException;
import in.bachatsetu.backend.storage.application.port.FileDeletePort;
import in.bachatsetu.backend.storage.application.port.TransactionPort;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import in.bachatsetu.backend.storage.domain.model.StoredFile;
import in.bachatsetu.backend.storage.domain.port.StorageRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteFileApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private StorageRepository repository;
    private FileDeletePort deletePort;
    private DeleteFileApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(StorageRepository.class);
        deletePort = mock(FileDeletePort.class);
        when(deletePort.supportedProvider()).thenReturn(StorageProvider.LOCAL);
        service = new DeleteFileApplicationService(repository, List.of(deletePort), new DirectTransactionPort());
    }

    @Test
    void removesThePhysicalObjectThenSoftDeletesMetadata() {
        AggregateId tenantId = AggregateId.newId();
        StoredFile file = newFile(tenantId);
        when(repository.findById(tenantId, file.id())).thenReturn(Optional.of(file));

        service.execute(tenantId, file.id());

        verify(deletePort).delete(file.path());
        verify(repository).delete(tenantId, file.id());
    }

    @Test
    void rejectsAnUnknownFile() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId fileId = AggregateId.newId();
        when(repository.findById(tenantId, fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(tenantId, fileId))
                .isInstanceOf(StoredFileNotFoundException.class);
        verify(deletePort, never()).delete(org.mockito.ArgumentMatchers.any());
        verify(repository, never()).delete(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private StoredFile newFile(AggregateId tenantId) {
        return StoredFile.upload(
                AggregateId.newId(), tenantId, StorageProvider.LOCAL, "/data/storage/tenant/file-1", "receipt.pdf",
                "application/pdf", 10L, "checksum", AggregateId.newId(), NOW);
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
