package in.bachatsetu.backend.storage.interfaces.rest.adapter;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.application.port.FileDeletePort;
import in.bachatsetu.backend.storage.application.port.FileDownloadPort;
import in.bachatsetu.backend.storage.application.port.StoragePort;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores files on the local filesystem under a configurable base directory, one subdirectory per tenant.
 * This is the only provider adapter in this module backed by a genuinely working implementation — Local
 * Storage requires no cloud credentials and is the safe default for every environment this codebase runs in.
 */
public final class LocalFileStorageAdapter implements StoragePort, FileDownloadPort, FileDeletePort {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileStorageAdapter.class);

    private final Path baseDirectory;

    public LocalFileStorageAdapter(String basePath) {
        this.baseDirectory = Path.of(Objects.requireNonNull(basePath, "basePath must not be null"));
    }

    @Override
    public StorageProvider supportedProvider() {
        return StorageProvider.LOCAL;
    }

    @Override
    public String store(AggregateId tenantId, AggregateId fileId, String filename, String contentType, byte[] content) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(fileId, "file id must not be null");
        Objects.requireNonNull(content, "content must not be null");
        try {
            Path tenantDirectory = baseDirectory.resolve(tenantId.value().toString());
            Files.createDirectories(tenantDirectory);
            Path target = tenantDirectory.resolve(fileId.value().toString());
            Files.write(target, content);
            LOGGER.info("Stored local file {} for tenant {}", fileId, tenantId);
            return target.toString();
        } catch (IOException exception) {
            throw new UncheckedIOException("failed to store file on the local filesystem", exception);
        }
    }

    @Override
    public byte[] download(String path) {
        Objects.requireNonNull(path, "path must not be null");
        try {
            return Files.readAllBytes(Path.of(path));
        } catch (IOException exception) {
            throw new UncheckedIOException("failed to read file from the local filesystem", exception);
        }
    }

    @Override
    public void delete(String path) {
        Objects.requireNonNull(path, "path must not be null");
        try {
            Files.deleteIfExists(Path.of(path));
            LOGGER.info("Deleted local file at {}", path);
        } catch (IOException exception) {
            throw new UncheckedIOException("failed to delete file from the local filesystem", exception);
        }
    }
}
