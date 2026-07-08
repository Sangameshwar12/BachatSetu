package in.bachatsetu.backend.receipt.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.receipt.application.usecase.GetReceiptPdfStorageUrlUseCase;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptPdfUseCase;
import in.bachatsetu.backend.storage.application.usecase.UploadFileUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ReceiptStorageIntegrationConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ReceiptStorageIntegrationConfig.class)
            .withBean(GetReceiptPdfUseCase.class, () -> (tenantId, id) -> null)
            .withBean(UploadFileUseCase.class, () -> command -> null);

    @Test
    void doesNotWireTheUseCaseByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(GetReceiptPdfStorageUrlUseCase.class);
        });
    }

    @Test
    void wiresTheUseCaseWhenExplicitlyEnabled() {
        contextRunner
                .withPropertyValues("bachatsetu.receipt.storage-upload.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(GetReceiptPdfStorageUrlUseCase.class);
                });
    }

    @Test
    void doesNotWireTheUseCaseWhenPersistenceRepositoriesDisabled() {
        contextRunner
                .withPropertyValues(
                        "bachatsetu.receipt.storage-upload.enabled=true",
                        "bachatsetu.persistence.repositories.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(GetReceiptPdfStorageUrlUseCase.class);
                });
    }
}
