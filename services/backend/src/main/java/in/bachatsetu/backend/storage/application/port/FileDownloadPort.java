package in.bachatsetu.backend.storage.application.port;

import in.bachatsetu.backend.storage.domain.model.StorageProvider;

/** Reads back the bytes previously stored at a provider-specific path. */
public interface FileDownloadPort {

    StorageProvider supportedProvider();

    byte[] download(String path);
}
