package in.bachatsetu.backend.storage.interfaces.rest.adapter;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.application.port.FileDeletePort;
import in.bachatsetu.backend.storage.application.port.FileDownloadPort;
import in.bachatsetu.backend.storage.application.port.StoragePort;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simulates AWS S3 object storage. **No AWS SDK or network call is used** — this is adapter structure only,
 * following the Payment Gateway simulated adapters pattern from Sprint 12.2: a deterministic-looking fake
 * object key is generated and logged, exactly what a real {@code S3Client}-backed adapter would return, so
 * provider selection, wiring, and the rest of this module's orchestration are present and testable ahead of
 * real SDK integration, without requiring live AWS credentials in this environment. Because no bytes are
 * actually persisted anywhere, {@link #download(String)} and {@link #delete(String)} are placeholders too:
 * they log what a real call would do rather than returning fabricated file content.
 */
public final class SimulatedAwsS3StorageAdapter implements StoragePort, FileDownloadPort, FileDeletePort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedAwsS3StorageAdapter.class);

    @Override
    public StorageProvider supportedProvider() {
        return StorageProvider.AWS_S3;
    }

    @Override
    public String store(AggregateId tenantId, AggregateId fileId, String filename, String contentType, byte[] content) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(fileId, "file id must not be null");
        Objects.requireNonNull(content, "content must not be null");
        String key = "s3://bachatsetu-storage/" + tenantId.value() + "/" + fileId.value();
        LOGGER.info("Simulated AWS S3 object {} stored for tenant {}", key, tenantId);
        return key;
    }

    @Override
    public byte[] download(String path) {
        Objects.requireNonNull(path, "path must not be null");
        LOGGER.info("Simulated AWS S3 download requested for {} (no live SDK call)", path);
        return new byte[0];
    }

    @Override
    public void delete(String path) {
        Objects.requireNonNull(path, "path must not be null");
        LOGGER.info("Simulated AWS S3 delete requested for {} (no live SDK call)", path);
    }
}
