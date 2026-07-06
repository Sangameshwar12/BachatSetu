package in.bachatsetu.backend.group.application.usecase;

import in.bachatsetu.backend.group.application.query.SavingsGroupSummary;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;

/** Lists compact savings group views within a tenant. */
@FunctionalInterface
public interface ListSavingsGroupsUseCase {

    List<SavingsGroupSummary> execute(AggregateId tenantId);
}
