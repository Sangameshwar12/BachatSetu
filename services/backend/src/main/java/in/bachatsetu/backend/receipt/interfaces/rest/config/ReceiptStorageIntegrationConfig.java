package in.bachatsetu.backend.receipt.interfaces.rest.config;

import in.bachatsetu.backend.receipt.application.service.GetReceiptPdfStorageUrlApplicationService;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptPdfStorageUrlUseCase;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptPdfUseCase;
import in.bachatsetu.backend.storage.application.usecase.UploadFileUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the optional Receipt PDF → Storage integration. Disabled by default: this bean, and therefore the
 * {@code ReceiptPdfStorageController} endpoint that depends on it, only exist when an operator explicitly
 * sets {@code bachatsetu.receipt.storage-upload.enabled=true}. Neither the pre-existing
 * {@link GetReceiptPdfUseCase} nor its REST endpoint are touched by this class.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ReceiptStorageIntegrationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "bachatsetu.receipt.storage-upload", name = "enabled", havingValue = "true")
    public GetReceiptPdfStorageUrlUseCase getReceiptPdfStorageUrlUseCase(
            GetReceiptPdfUseCase getReceiptPdf, UploadFileUseCase uploadFile) {
        return new GetReceiptPdfStorageUrlApplicationService(getReceiptPdf, uploadFile);
    }
}
