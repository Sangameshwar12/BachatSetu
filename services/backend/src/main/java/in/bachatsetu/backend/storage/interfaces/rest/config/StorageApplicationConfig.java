package in.bachatsetu.backend.storage.interfaces.rest.config;

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
import in.bachatsetu.backend.storage.application.usecase.DeleteFileUseCase;
import in.bachatsetu.backend.storage.application.usecase.DownloadFileUseCase;
import in.bachatsetu.backend.storage.application.usecase.GetFileMetadataUseCase;
import in.bachatsetu.backend.storage.application.usecase.UploadFileUseCase;
import in.bachatsetu.backend.storage.domain.port.StorageRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes framework-free Storage application services.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled}, matching every other module's application
 * config.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class StorageApplicationConfig {

    @Bean
    public StorageApplicationMapper storageApplicationMapper() {
        return new StorageApplicationMapper();
    }

    @Bean
    public UploadFileUseCase uploadFileUseCase(
            StorageRepository repository,
            List<StoragePort> storagePorts,
            ChecksumGeneratorPort checksumGenerator,
            ClockPort clock,
            TransactionPort transaction,
            StorageApplicationMapper mapper,
            StorageProperties properties,
            CreateAuditEntryUseCase createAuditEntry) {
        return new UploadFileApplicationService(
                repository, storagePorts, checksumGenerator, clock, transaction, mapper,
                properties.defaultProvider(), createAuditEntry);
    }

    @Bean
    public DownloadFileUseCase downloadFileUseCase(
            StorageRepository repository, List<FileDownloadPort> downloadPorts, TransactionPort transaction) {
        return new DownloadFileApplicationService(repository, downloadPorts, transaction);
    }

    @Bean
    public DeleteFileUseCase deleteFileUseCase(
            StorageRepository repository,
            List<FileDeletePort> deletePorts,
            TransactionPort transaction,
            CreateAuditEntryUseCase createAuditEntry) {
        return new DeleteFileApplicationService(repository, deletePorts, transaction, createAuditEntry);
    }

    @Bean
    public GetFileMetadataUseCase getFileMetadataUseCase(
            StorageRepository repository, TransactionPort transaction, StorageApplicationMapper mapper) {
        return new GetFileMetadataApplicationService(repository, transaction, mapper);
    }
}
