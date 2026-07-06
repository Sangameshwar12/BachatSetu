package in.bachatsetu.backend.group.application.usecase;

import in.bachatsetu.backend.group.application.port.GroupPage;
import in.bachatsetu.backend.group.application.port.GroupPageRequest;
import in.bachatsetu.backend.group.application.query.SavingsGroupSummary;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Lists compact savings group views within a tenant, paginated at the persistence boundary. */
@FunctionalInterface
public interface ListSavingsGroupsUseCase {

    GroupPage<SavingsGroupSummary> execute(AggregateId tenantId, GroupPageRequest pageRequest);
}
