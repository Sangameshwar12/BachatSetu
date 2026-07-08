package in.bachatsetu.backend.storage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.application.exception.StoredFileNotFoundException;
import in.bachatsetu.backend.storage.application.mapper.StorageApplicationMapper;
import in.bachatsetu.backend.storage.application.port.TransactionPort;
import in.bachatsetu.backend.storage.application.query.StoredFileResult;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import in.bachatsetu.backend.storage.domain.model.StoredFile;
import in.bachatsetu.backend.storage.domain.port.StorageRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetFileMetadataApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private StorageRepository repository;
    private GetFileMetadataApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(StorageRepository.class);
        service = new GetFileMetadataApplicationService(
                repository, new DirectTransactionPort(), new StorageApplicationMapper());
    }

    @Test
    void returnsMetadataForAnExistingFile() {
        AggregateId tenantId = AggregateId.newId();
        StoredFile file = StoredFile.upload(
                AggregateId.newId(), tenantId, StorageProvider.LOCAL, "/data/storage/tenant/file-1", "receipt.pdf",
                "application/pdf", 10L, "checksum", AggregateId.newId(), NOW);
        when(repository.findById(tenantId, file.id())).thenReturn(Optional.of(file));

        StoredFileResult result = service.execute(tenantId, file.id());

        assertThat(result.originalFilename()).isEqualTo("receipt.pdf");
        assertThat(result.checksum()).isEqualTo("checksum");
    }

    @Test
    void rejectsAnUnknownFile() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId fileId = AggregateId.newId();
        when(repository.findById(tenantId, fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(tenantId, fileId))
                .isInstanceOf(StoredFileNotFoundException.class);
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
