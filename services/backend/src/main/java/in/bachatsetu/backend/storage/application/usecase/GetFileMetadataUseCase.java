package in.bachatsetu.backend.storage.application.usecase;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.application.query.StoredFileResult;

/** Returns one tenant-scoped file's metadata. */
@FunctionalInterface
public interface GetFileMetadataUseCase {

    StoredFileResult execute(AggregateId tenantId, AggregateId fileId);
}
