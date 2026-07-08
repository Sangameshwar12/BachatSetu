package in.bachatsetu.backend.storage.interfaces.rest.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void roundTripsStoreDownloadAndDelete() {
        LocalFileStorageAdapter adapter = new LocalFileStorageAdapter(tempDir.toString());
        AggregateId tenantId = AggregateId.newId();
        AggregateId fileId = AggregateId.newId();
        byte[] content = "hello local storage".getBytes();

        String path = adapter.store(tenantId, fileId, "file.txt", "text/plain", content);

        assertThat(adapter.supportedProvider()).isEqualTo(StorageProvider.LOCAL);
        assertThat(Files.exists(Paths.get(path))).isTrue();
        assertThat(adapter.download(path)).isEqualTo(content);

        adapter.delete(path);

        assertThat(Files.exists(Paths.get(path))).isFalse();
    }

    @Test
    void storesFilesUnderPerTenantSubdirectories() {
        LocalFileStorageAdapter adapter = new LocalFileStorageAdapter(tempDir.toString());
        AggregateId tenantId = AggregateId.newId();
        AggregateId fileId = AggregateId.newId();

        String path = adapter.store(tenantId, fileId, "file.txt", "text/plain", "data".getBytes());

        assertThat(path).contains(tenantId.value().toString());
        assertThat(path).contains(fileId.value().toString());
    }

    @Test
    void deletingAMissingFileDoesNotThrow() {
        LocalFileStorageAdapter adapter = new LocalFileStorageAdapter(tempDir.toString());

        adapter.delete(tempDir.resolve("does-not-exist").toString());
    }
}
