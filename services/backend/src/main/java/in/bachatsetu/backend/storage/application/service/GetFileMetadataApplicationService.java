package in.bachatsetu.backend.storage.application.service;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.application.exception.StoredFileNotFoundException;
import in.bachatsetu.backend.storage.application.mapper.StorageApplicationMapper;
import in.bachatsetu.backend.storage.application.port.TransactionPort;
import in.bachatsetu.backend.storage.application.query.StoredFileResult;
import in.bachatsetu.backend.storage.application.usecase.GetFileMetadataUseCase;
import in.bachatsetu.backend.storage.domain.model.StoredFile;
import in.bachatsetu.backend.storage.domain.port.StorageRepository;
import java.util.Objects;

/** Retrieves and maps a tenant-scoped {@link StoredFile} aggregate. */
public final class GetFileMetadataApplicationService implements GetFileMetadataUseCase {

    private final StorageRepository repository;
    private final TransactionPort transaction;
    private final StorageApplicationMapper mapper;

    public GetFileMetadataApplicationService(
            StorageRepository repository, TransactionPort transaction, StorageApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public StoredFileResult execute(AggregateId tenantId, AggregateId fileId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(fileId, "file id must not be null");
        return transaction.execute(() -> {
            StoredFile file = repository.findById(tenantId, fileId)
                    .orElseThrow(() -> new StoredFileNotFoundException("stored file does not exist"));
            return mapper.toMetadataResult(file);
        });
    }
}
