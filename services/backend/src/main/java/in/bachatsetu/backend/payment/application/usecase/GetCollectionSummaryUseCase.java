package in.bachatsetu.backend.payment.application.usecase;

import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.payment.application.query.CollectionSummaryResult;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Retrieves a group's current-cycle contribution collection status. */
@FunctionalInterface
public interface GetCollectionSummaryUseCase {

    CollectionSummaryResult execute(AggregateId tenantId, GroupId groupId);
}
