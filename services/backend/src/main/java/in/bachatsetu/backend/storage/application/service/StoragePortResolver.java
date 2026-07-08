package in.bachatsetu.backend.storage.application.service;

import in.bachatsetu.backend.storage.application.exception.UnsupportedStorageProviderException;
import in.bachatsetu.backend.storage.application.port.FileDeletePort;
import in.bachatsetu.backend.storage.application.port.FileDownloadPort;
import in.bachatsetu.backend.storage.application.port.StoragePort;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import java.util.List;

/**
 * Selects the one adapter matching a requested {@link StorageProvider} out of every adapter Spring wires for
 * a given port — the mechanism behind provider abstraction and provider switching: application code never
 * names a concrete Local/S3/Azure/GCS class, only a {@link StorageProvider}.
 */
final class StoragePortResolver {

    private StoragePortResolver() {
    }

    static StoragePort resolveStoragePort(List<StoragePort> ports, StorageProvider provider) {
        return ports.stream()
                .filter(port -> port.supportedProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new UnsupportedStorageProviderException(
                        "no storage adapter registered for " + provider));
    }

    static FileDownloadPort resolveDownloadPort(List<FileDownloadPort> ports, StorageProvider provider) {
        return ports.stream()
                .filter(port -> port.supportedProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new UnsupportedStorageProviderException(
                        "no download adapter registered for " + provider));
    }

    static FileDeletePort resolveDeletePort(List<FileDeletePort> ports, StorageProvider provider) {
        return ports.stream()
                .filter(port -> port.supportedProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new UnsupportedStorageProviderException(
                        "no delete adapter registered for " + provider));
    }
}
