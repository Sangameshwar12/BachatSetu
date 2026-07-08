package in.bachatsetu.backend.storage.interfaces.rest.config;

import in.bachatsetu.backend.storage.application.port.ChecksumGeneratorPort;
import in.bachatsetu.backend.storage.application.port.ClockPort;
import in.bachatsetu.backend.storage.application.port.FileDeletePort;
import in.bachatsetu.backend.storage.application.port.FileDownloadPort;
import in.bachatsetu.backend.storage.application.port.StoragePort;
import in.bachatsetu.backend.storage.application.port.TransactionPort;
import in.bachatsetu.backend.storage.interfaces.rest.adapter.LocalFileStorageAdapter;
import in.bachatsetu.backend.storage.interfaces.rest.adapter.Sha256ChecksumGeneratorAdapter;
import in.bachatsetu.backend.storage.interfaces.rest.adapter.SimulatedAwsS3StorageAdapter;
import in.bachatsetu.backend.storage.interfaces.rest.adapter.SimulatedAzureBlobStorageAdapter;
import in.bachatsetu.backend.storage.interfaces.rest.adapter.SimulatedGoogleCloudStorageAdapter;
import in.bachatsetu.backend.storage.interfaces.rest.adapter.SpringStorageTransactionAdapter;
import in.bachatsetu.backend.storage.interfaces.rest.adapter.SystemStorageClockAdapter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Composes the Storage outbound port adapters: the standard Clock/Transaction trio, a real SHA-256 checksum
 * generator, and one adapter per provider — Local (a genuinely working filesystem adapter) plus simulated
 * AWS S3/Azure Blob/Google Cloud Storage adapters. Each adapter is registered as a single bean of its
 * concrete type; since every adapter implements {@link StoragePort}, {@link FileDownloadPort}, and
 * {@link FileDeletePort} simultaneously, Spring's collection-typed injection (application code depends on
 * {@code List<StoragePort>} etc.) picks it up under all three without any separate wrapper bean per
 * interface — declaring those separately would register the same instance under extra bean names and make
 * {@code List<StoragePort>} etc. report duplicates.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled}, matching every other module's
 * infrastructure config, for the same non-deterministic-condition-evaluation-order reason documented on
 * {@code PaymentGatewayInfrastructureConfig}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StorageProperties.class)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class StorageInfrastructureConfig {

    @Bean
    Clock storageClock() {
        return Clock.systemUTC();
    }

    @Bean
    TransactionTemplate storageTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    ClockPort systemStorageClockAdapter(Clock storageClock) {
        return new SystemStorageClockAdapter(storageClock);
    }

    @Bean
    TransactionPort springStorageTransactionAdapter(TransactionTemplate storageTransactionTemplate) {
        return new SpringStorageTransactionAdapter(storageTransactionTemplate);
    }

    @Bean
    ChecksumGeneratorPort sha256ChecksumGeneratorAdapter() {
        return new Sha256ChecksumGeneratorAdapter();
    }

    @Bean
    LocalFileStorageAdapter localFileStorageAdapter(StorageProperties properties) {
        return new LocalFileStorageAdapter(properties.local().path());
    }

    @Bean
    SimulatedAwsS3StorageAdapter simulatedAwsS3StorageAdapter() {
        return new SimulatedAwsS3StorageAdapter();
    }

    @Bean
    SimulatedAzureBlobStorageAdapter simulatedAzureBlobStorageAdapter() {
        return new SimulatedAzureBlobStorageAdapter();
    }

    @Bean
    SimulatedGoogleCloudStorageAdapter simulatedGoogleCloudStorageAdapter() {
        return new SimulatedGoogleCloudStorageAdapter();
    }
}
