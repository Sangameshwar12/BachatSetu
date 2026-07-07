package in.bachatsetu.backend.draw.application.usecase;

import in.bachatsetu.backend.draw.application.query.DrawResult;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Retrieves one tenant-scoped draw. */
@FunctionalInterface
public interface GetDrawUseCase {

    DrawResult execute(AggregateId tenantId, AggregateId drawId);
}
