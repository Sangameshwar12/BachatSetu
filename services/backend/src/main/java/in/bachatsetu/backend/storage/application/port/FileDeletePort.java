package in.bachatsetu.backend.storage.application.port;

import in.bachatsetu.backend.storage.domain.model.StorageProvider;

/** Removes the physical object previously stored at a provider-specific path. */
public interface FileDeletePort {

    StorageProvider supportedProvider();

    void delete(String path);
}
