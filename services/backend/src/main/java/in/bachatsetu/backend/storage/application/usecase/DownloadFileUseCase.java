package in.bachatsetu.backend.storage.application.usecase;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.application.query.FileDownloadResult;

/** Returns one tenant-scoped file's bytes. */
@FunctionalInterface
public interface DownloadFileUseCase {

    FileDownloadResult execute(AggregateId tenantId, AggregateId fileId);
}
