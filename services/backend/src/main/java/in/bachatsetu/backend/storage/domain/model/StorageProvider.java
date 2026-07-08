package in.bachatsetu.backend.storage.domain.model;

/** The physical storage backend a {@code StoredFile} was uploaded to. */
public enum StorageProvider {
    LOCAL,
    AWS_S3,
    AZURE_BLOB,
    GOOGLE_CLOUD_STORAGE
}
