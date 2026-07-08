package in.bachatsetu.backend.storage.interfaces.rest.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import org.junit.jupiter.api.Test;

class SimulatedStorageAdapterTest {

    @Test
    void awsS3AdapterGeneratesADistinctKeyPerCallAndReportsItsProvider() {
        SimulatedAwsS3StorageAdapter adapter = new SimulatedAwsS3StorageAdapter();
        AggregateId tenantId = AggregateId.newId();

        String first = adapter.store(tenantId, AggregateId.newId(), "a.txt", "text/plain", "x".getBytes());
        String second = adapter.store(tenantId, AggregateId.newId(), "a.txt", "text/plain", "x".getBytes());

        assertThat(adapter.supportedProvider()).isEqualTo(StorageProvider.AWS_S3);
        assertThat(first).isNotEqualTo(second);
        assertThat(first).startsWith("s3://");
        assertThat(adapter.download(first)).isEmpty();
        adapter.delete(first);
    }

    @Test
    void azureBlobAdapterGeneratesAKeyAndReportsItsProvider() {
        SimulatedAzureBlobStorageAdapter adapter = new SimulatedAzureBlobStorageAdapter();
        AggregateId tenantId = AggregateId.newId();

        String path = adapter.store(tenantId, AggregateId.newId(), "a.txt", "text/plain", "x".getBytes());

        assertThat(adapter.supportedProvider()).isEqualTo(StorageProvider.AZURE_BLOB);
        assertThat(path).contains("blob.core.windows.net");
        assertThat(adapter.download(path)).isEmpty();
        adapter.delete(path);
    }

    @Test
    void googleCloudStorageAdapterGeneratesAKeyAndReportsItsProvider() {
        SimulatedGoogleCloudStorageAdapter adapter = new SimulatedGoogleCloudStorageAdapter();
        AggregateId tenantId = AggregateId.newId();

        String path = adapter.store(tenantId, AggregateId.newId(), "a.txt", "text/plain", "x".getBytes());

        assertThat(adapter.supportedProvider()).isEqualTo(StorageProvider.GOOGLE_CLOUD_STORAGE);
        assertThat(path).startsWith("gs://");
        assertThat(adapter.download(path)).isEmpty();
        adapter.delete(path);
    }
}
