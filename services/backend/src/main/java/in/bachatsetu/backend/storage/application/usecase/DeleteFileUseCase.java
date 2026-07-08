package in.bachatsetu.backend.storage.application.usecase;

import in.bachatsetu.backend.shared.domain.AggregateId;

/** Removes the physical object and soft-deletes its metadata. */
@FunctionalInterface
public interface DeleteFileUseCase {

    void execute(AggregateId tenantId, AggregateId fileId);
}
