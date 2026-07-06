package in.bachatsetu.backend.group.application.usecase;

import in.bachatsetu.backend.group.application.query.SavingsGroupResult;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Retrieves one tenant-scoped savings group. */
@FunctionalInterface
public interface GetSavingsGroupUseCase {

    SavingsGroupResult execute(AggregateId tenantId, GroupId groupId);
}
