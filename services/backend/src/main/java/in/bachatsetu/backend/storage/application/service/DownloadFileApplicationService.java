package in.bachatsetu.backend.storage.application.service;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.application.exception.StoredFileNotFoundException;
import in.bachatsetu.backend.storage.application.port.FileDownloadPort;
import in.bachatsetu.backend.storage.application.port.TransactionPort;
import in.bachatsetu.backend.storage.application.query.FileDownloadResult;
import in.bachatsetu.backend.storage.application.usecase.DownloadFileUseCase;
import in.bachatsetu.backend.storage.domain.model.StoredFile;
import in.bachatsetu.backend.storage.domain.port.StorageRepository;
import java.util.List;
import java.util.Objects;

/** Loads a tenant-scoped file's metadata and reads its bytes back from the provider that stored it. */
public final class DownloadFileApplicationService implements DownloadFileUseCase {

    private final StorageRepository repository;
    private final List<FileDownloadPort> downloadPorts;
    private final TransactionPort transaction;

    public DownloadFileApplicationService(
            StorageRepository repository, List<FileDownloadPort> downloadPorts, TransactionPort transaction) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.downloadPorts = List.copyOf(Objects.requireNonNull(downloadPorts, "downloadPorts must not be null"));
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
    }

    @Override
    public FileDownloadResult execute(AggregateId tenantId, AggregateId fileId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(fileId, "file id must not be null");
        return transaction.execute(() -> download(tenantId, fileId));
    }

    private FileDownloadResult download(AggregateId tenantId, AggregateId fileId) {
        StoredFile file = repository.findById(tenantId, fileId)
                .orElseThrow(() -> new StoredFileNotFoundException("stored file does not exist"));
        FileDownloadPort downloadPort = StoragePortResolver.resolveDownloadPort(downloadPorts, file.provider());
        byte[] content = downloadPort.download(file.path());
        return new FileDownloadResult(file.id().value(), file.originalFilename(), file.contentType(), content);
    }
}
