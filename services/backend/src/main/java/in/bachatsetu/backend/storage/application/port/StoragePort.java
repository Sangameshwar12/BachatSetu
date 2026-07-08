package in.bachatsetu.backend.storage.application.port;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;

/** Stores one file's bytes with a given provider and returns the provider-specific path/key it was stored under. */
public interface StoragePort {

    StorageProvider supportedProvider();

    String store(AggregateId tenantId, AggregateId fileId, String filename, String contentType, byte[] content);
}
