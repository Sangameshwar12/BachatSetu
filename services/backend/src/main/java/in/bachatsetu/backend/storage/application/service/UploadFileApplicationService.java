package in.bachatsetu.backend.storage.application.service;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.application.command.UploadFileCommand;
import in.bachatsetu.backend.storage.application.mapper.StorageApplicationMapper;
import in.bachatsetu.backend.storage.application.port.ChecksumGeneratorPort;
import in.bachatsetu.backend.storage.application.port.ClockPort;
import in.bachatsetu.backend.storage.application.port.StoragePort;
import in.bachatsetu.backend.storage.application.port.TransactionPort;
import in.bachatsetu.backend.storage.application.query.UploadFileResult;
import in.bachatsetu.backend.storage.application.usecase.UploadFileUseCase;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import in.bachatsetu.backend.storage.domain.model.StoredFile;
import in.bachatsetu.backend.storage.domain.port.StorageRepository;
import java.util.List;
import java.util.Objects;

/**
 * Stores a new file's bytes through the configured default provider and records its metadata. The checksum
 * is computed once, here, from the original bytes — the same value is persisted on {@link StoredFile} and
 * never recomputed from provider-returned data, so it always reflects exactly what the caller uploaded.
 */
public final class UploadFileApplicationService implements UploadFileUseCase {

    private final StorageRepository repository;
    private final List<StoragePort> storagePorts;
    private final ChecksumGeneratorPort checksumGenerator;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final StorageApplicationMapper mapper;
    private final StorageProvider defaultProvider;
    private final CreateAuditEntryUseCase createAuditEntry;

    public UploadFileApplicationService(
            StorageRepository repository,
            List<StoragePort> storagePorts,
            ChecksumGeneratorPort checksumGenerator,
            ClockPort clock,
            TransactionPort transaction,
            StorageApplicationMapper mapper,
            StorageProvider defaultProvider,
            CreateAuditEntryUseCase createAuditEntry) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.storagePorts = List.copyOf(Objects.requireNonNull(storagePorts, "storagePorts must not be null"));
        this.checksumGenerator = Objects.requireNonNull(checksumGenerator, "checksum generator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.defaultProvider = Objects.requireNonNull(defaultProvider, "default provider must not be null");
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @Override
    public UploadFileResult execute(UploadFileCommand command) {
        Objects.requireNonNull(command, "upload command must not be null");
        UploadFileResult result = transaction.execute(() -> upload(command));
        auditFileUploaded(command, result);
        return result;
    }

    private UploadFileResult upload(UploadFileCommand command) {
        StoragePort storagePort = StoragePortResolver.resolveStoragePort(storagePorts, defaultProvider);
        byte[] content = command.content();
        String checksum = checksumGenerator.generate(content);
        AggregateId fileId = AggregateId.newId();
        String path = storagePort.store(command.tenantId(), fileId, command.filename(), command.contentType(), content);
        StoredFile file = StoredFile.upload(
                fileId, command.tenantId(), defaultProvider, path, command.filename(), command.contentType(),
                content.length, checksum, command.actorId(), clock.now());
        repository.save(file);
        return mapper.toUploadResult(file);
    }

    /**
     * Best-effort: an audit failure must never fail an upload that has already committed, so any exception is
     * caught and discarded here rather than propagated.
     */
    private void auditFileUploaded(UploadFileCommand command, UploadFileResult result) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    command.tenantId(), command.actorId(), AuditEventType.FILE_UPLOADED, "storage", "StoredFile",
                    new AggregateId(result.fileId()), "FILE_UPLOADED", "file uploaded", null, null, null));
        } catch (RuntimeException exception) {
            // Audit is best-effort: never let a recording failure affect an already-uploaded file.
        }
    }
}
