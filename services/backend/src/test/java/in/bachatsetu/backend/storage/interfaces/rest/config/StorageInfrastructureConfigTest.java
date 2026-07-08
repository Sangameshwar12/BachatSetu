package in.bachatsetu.backend.storage.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.storage.application.port.ChecksumGeneratorPort;
import in.bachatsetu.backend.storage.application.port.ClockPort;
import in.bachatsetu.backend.storage.application.port.FileDeletePort;
import in.bachatsetu.backend.storage.application.port.FileDownloadPort;
import in.bachatsetu.backend.storage.application.port.StoragePort;
import in.bachatsetu.backend.storage.application.port.TransactionPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

class StorageInfrastructureConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StorageInfrastructureConfig.class);

    @Test
    void wiresEveryPortAndProviderAdapterWhenTransactionManagerIsAvailable() {
        contextRunner
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .withPropertyValues(
                        "bachatsetu.storage.enabled=true",
                        "bachatsetu.storage.default-provider=LOCAL",
                        "bachatsetu.storage.local.path=./target/test-storage",
                        "bachatsetu.storage.aws.bucket=",
                        "bachatsetu.storage.aws.region=",
                        "bachatsetu.storage.aws.access-key-id=",
                        "bachatsetu.storage.aws.secret-access-key=",
                        "bachatsetu.storage.azure.account-name=",
                        "bachatsetu.storage.azure.account-key=",
                        "bachatsetu.storage.azure.container-name=",
                        "bachatsetu.storage.gcp.bucket=",
                        "bachatsetu.storage.gcp.project-id=",
                        "bachatsetu.storage.gcp.credentials-json=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ClockPort.class);
                    assertThat(context).hasSingleBean(TransactionPort.class);
                    assertThat(context).hasSingleBean(ChecksumGeneratorPort.class);
                    assertThat(context.getBeansOfType(StoragePort.class)).hasSize(4);
                    assertThat(context.getBeansOfType(FileDownloadPort.class)).hasSize(4);
                    assertThat(context.getBeansOfType(FileDeletePort.class)).hasSize(4);
                });
    }

    @Test
    void doesNotWireAdaptersWhenPersistenceRepositoriesDisabled() {
        contextRunner
                .withPropertyValues("bachatsetu.persistence.repositories.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ClockPort.class);
                    assertThat(context.getBeansOfType(StoragePort.class)).isEmpty();
                });
    }
}
