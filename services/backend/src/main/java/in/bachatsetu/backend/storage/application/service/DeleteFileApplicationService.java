package in.bachatsetu.backend.storage.application.service;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.application.exception.StoredFileNotFoundException;
import in.bachatsetu.backend.storage.application.port.FileDeletePort;
import in.bachatsetu.backend.storage.application.port.TransactionPort;
import in.bachatsetu.backend.storage.application.usecase.DeleteFileUseCase;
import in.bachatsetu.backend.storage.domain.model.StoredFile;
import in.bachatsetu.backend.storage.domain.port.StorageRepository;
import java.util.List;
import java.util.Objects;

/**
 * Removes the physical object through the provider that stored it, then soft-deletes the metadata. The
 * physical removal happens first: if it fails, the metadata is left intact so the operation can be retried
 * against a file that still genuinely exists.
 */
public final class DeleteFileApplicationService implements DeleteFileUseCase {

    private final StorageRepository repository;
    private final List<FileDeletePort> deletePorts;
    private final TransactionPort transaction;
    private final CreateAuditEntryUseCase createAuditEntry;

    public DeleteFileApplicationService(
            StorageRepository repository,
            List<FileDeletePort> deletePorts,
            TransactionPort transaction,
            CreateAuditEntryUseCase createAuditEntry) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.deletePorts = List.copyOf(Objects.requireNonNull(deletePorts, "deletePorts must not be null"));
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @Override
    public void execute(AggregateId tenantId, AggregateId fileId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(fileId, "file id must not be null");
        transaction.execute(() -> {
            delete(tenantId, fileId);
            return null;
        });
        auditFileDeleted(tenantId, fileId);
    }

    private void delete(AggregateId tenantId, AggregateId fileId) {
        StoredFile file = repository.findById(tenantId, fileId)
                .orElseThrow(() -> new StoredFileNotFoundException("stored file does not exist"));
        FileDeletePort deletePort = StoragePortResolver.resolveDeletePort(deletePorts, file.provider());
        deletePort.delete(file.path());
        repository.delete(tenantId, fileId);
    }

    /**
     * Best-effort: an audit failure must never fail a delete that has already committed, so any exception is
     * caught and discarded here rather than propagated.
     */
    private void auditFileDeleted(AggregateId tenantId, AggregateId fileId) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    tenantId, null, AuditEventType.FILE_DELETED, "storage", "StoredFile", fileId, "FILE_DELETED",
                    "file deleted", null, null, null));
        } catch (RuntimeException exception) {
            // Audit is best-effort: never let a recording failure affect an already-deleted file.
        }
    }
}
