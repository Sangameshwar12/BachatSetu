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
 * Simulates Google Cloud Storage. **No Google Cloud SDK or network call is used** — adapter structure only,
 * following the same simulated-adapter pattern as {@link SimulatedAwsS3StorageAdapter}. See that class's
 * Javadoc for the full rationale, including why {@link #download(String)} and {@link #delete(String)} are
 * placeholders.
 */
public final class SimulatedGoogleCloudStorageAdapter implements StoragePort, FileDownloadPort, FileDeletePort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedGoogleCloudStorageAdapter.class);

    @Override
    public StorageProvider supportedProvider() {
        return StorageProvider.GOOGLE_CLOUD_STORAGE;
    }

    @Override
    public String store(AggregateId tenantId, AggregateId fileId, String filename, String contentType, byte[] content) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(fileId, "file id must not be null");
        Objects.requireNonNull(content, "content must not be null");
        String key = "gs://bachatsetu-storage/" + tenantId.value() + "/" + fileId.value();
        LOGGER.info("Simulated Google Cloud Storage object {} stored for tenant {}", key, tenantId);
        return key;
    }

    @Override
    public byte[] download(String path) {
        Objects.requireNonNull(path, "path must not be null");
        LOGGER.info("Simulated Google Cloud Storage download requested for {} (no live SDK call)", path);
        return new byte[0];
    }

    @Override
    public void delete(String path) {
        Objects.requireNonNull(path, "path must not be null");
        LOGGER.info("Simulated Google Cloud Storage delete requested for {} (no live SDK call)", path);
    }
}
