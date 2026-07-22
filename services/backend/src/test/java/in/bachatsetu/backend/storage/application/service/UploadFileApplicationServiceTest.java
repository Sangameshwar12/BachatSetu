package in.bachatsetu.backend.storage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.application.command.UploadFileCommand;
import in.bachatsetu.backend.storage.application.mapper.StorageApplicationMapper;
import in.bachatsetu.backend.storage.application.port.ChecksumGeneratorPort;
import in.bachatsetu.backend.storage.application.port.ClockPort;
import in.bachatsetu.backend.storage.application.port.StoragePort;
import in.bachatsetu.backend.storage.application.port.TransactionPort;
import in.bachatsetu.backend.storage.application.query.UploadFileResult;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import in.bachatsetu.backend.storage.domain.model.StoredFile;
import in.bachatsetu.backend.storage.domain.port.StorageRepository;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UploadFileApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private StorageRepository repository;
    private StoragePort storagePort;
    private ChecksumGeneratorPort checksumGenerator;
    private ClockPort clock;
    private TransactionPort transaction;
    private StorageApplicationMapper mapper;
    private CreateAuditEntryUseCase createAuditEntry;
    private UploadFileApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(StorageRepository.class);
        storagePort = mock(StoragePort.class);
        when(storagePort.supportedProvider()).thenReturn(StorageProvider.LOCAL);
        checksumGenerator = mock(ChecksumGeneratorPort.class);
        clock = () -> NOW;
        transaction = new DirectTransactionPort();
        mapper = new StorageApplicationMapper();
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        service = new UploadFileApplicationService(
                repository, List.of(storagePort), checksumGenerator, clock, transaction, mapper,
                StorageProvider.LOCAL, createAuditEntry);
    }

    @Test
    void uploadsAFileThroughTheDefaultProvider() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        byte[] content = "hello world".getBytes();
        when(checksumGenerator.generate(content)).thenReturn("checksum-1");
        when(storagePort.store(eq(tenantId), any(), eq("receipt.pdf"), eq("application/pdf"), eq(content)))
                .thenReturn("/data/storage/tenant/file-1");

        UploadFileResult result = service.execute(
                new UploadFileCommand(tenantId, "receipt.pdf", "application/pdf", content, actorId));

        assertThat(result.provider()).isEqualTo(StorageProvider.LOCAL);
        assertThat(result.path()).isEqualTo("/data/storage/tenant/file-1");
        ArgumentCaptor<StoredFile> captor = ArgumentCaptor.forClass(StoredFile.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().checksum()).isEqualTo("checksum-1");
        assertThat(captor.getValue().size()).isEqualTo(content.length);
        verify(createAuditEntry).execute(any());
    }

    @Test
    void acceptsEveryAllowedImageAndDocumentContentType() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        byte[] content = "image-bytes".getBytes();
        when(checksumGenerator.generate(content)).thenReturn("checksum-2");
        when(storagePort.store(eq(tenantId), any(), any(), any(), eq(content))).thenReturn("/data/storage/tenant/file-2");

        for (String contentType : List.of("image/jpeg", "image/png", "image/webp", "application/pdf")) {
            UploadFileResult result = service.execute(
                    new UploadFileCommand(tenantId, "upload", contentType, content, actorId));
            assertThat(result.provider()).isEqualTo(StorageProvider.LOCAL);
        }
    }

    @Test
    void rejectsAnUnsupportedContentType() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        byte[] content = "hello world".getBytes();

        assertThatThrownBy(() -> service.execute(
                        new UploadFileCommand(tenantId, "script.js", "application/javascript", content, actorId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("application/javascript");
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsNullCommand() {
        assertThatThrownBy(() -> service.execute(null)).isInstanceOf(NullPointerException.class);
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
