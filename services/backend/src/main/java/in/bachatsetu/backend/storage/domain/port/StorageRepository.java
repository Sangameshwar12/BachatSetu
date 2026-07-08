package in.bachatsetu.backend.storage.domain.port;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.domain.model.StoredFile;
import java.util.Optional;

public interface StorageRepository {

    Optional<StoredFile> findById(AggregateId tenantId, AggregateId fileId);

    void save(StoredFile file);

    /** Soft-deletes the file's metadata; the physical object is removed separately through a delete port. */
    void delete(AggregateId tenantId, AggregateId fileId);
}
