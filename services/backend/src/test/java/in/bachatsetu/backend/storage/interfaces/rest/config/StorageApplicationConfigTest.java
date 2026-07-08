package in.bachatsetu.backend.storage.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.storage.application.mapper.StorageApplicationMapper;
import in.bachatsetu.backend.storage.application.port.ChecksumGeneratorPort;
import in.bachatsetu.backend.storage.application.port.ClockPort;
import in.bachatsetu.backend.storage.application.port.FileDeletePort;
import in.bachatsetu.backend.storage.application.port.FileDownloadPort;
import in.bachatsetu.backend.storage.application.port.StoragePort;
import in.bachatsetu.backend.storage.application.port.TransactionPort;
import in.bachatsetu.backend.storage.application.service.DeleteFileApplicationService;
import in.bachatsetu.backend.storage.application.service.DownloadFileApplicationService;
import in.bachatsetu.backend.storage.application.service.GetFileMetadataApplicationService;
import in.bachatsetu.backend.storage.application.service.UploadFileApplicationService;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import in.bachatsetu.backend.storage.domain.port.StorageRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class StorageApplicationConfigTest {

    private final StorageApplicationConfig config = new StorageApplicationConfig();
    private final StorageApplicationMapper mapper = config.storageApplicationMapper();
    private final StorageRepository repository = mock(StorageRepository.class);
    private final ChecksumGeneratorPort checksumGenerator = mock(ChecksumGeneratorPort.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = mock(TransactionPort.class);
    private final StorageProperties properties = new StorageProperties(
            true, StorageProvider.LOCAL,
            new StorageProperties.Local("./target/test-storage"),
            new StorageProperties.Aws("", "", "", ""),
            new StorageProperties.Azure("", "", ""),
            new StorageProperties.Gcp("", "", ""));
    private final CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);

    @Test
    void composesUploadFileUseCase() {
        assertThat(config.uploadFileUseCase(
                        repository, List.<StoragePort>of(), checksumGenerator, clock, transaction, mapper,
                        properties, createAuditEntry))
                .isInstanceOf(UploadFileApplicationService.class);
    }

    @Test
    void composesDownloadFileUseCase() {
        assertThat(config.downloadFileUseCase(repository, List.<FileDownloadPort>of(), transaction))
                .isInstanceOf(DownloadFileApplicationService.class);
    }

    @Test
    void composesDeleteFileUseCase() {
        assertThat(config.deleteFileUseCase(repository, List.<FileDeletePort>of(), transaction, createAuditEntry))
                .isInstanceOf(DeleteFileApplicationService.class);
    }

    @Test
    void composesGetFileMetadataUseCase() {
        assertThat(config.getFileMetadataUseCase(repository, transaction, mapper))
                .isInstanceOf(GetFileMetadataApplicationService.class);
    }
}
