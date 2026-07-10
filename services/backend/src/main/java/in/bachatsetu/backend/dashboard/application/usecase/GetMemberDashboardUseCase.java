package in.bachatsetu.backend.dashboard.application.usecase;

import in.bachatsetu.backend.dashboard.application.query.MemberDashboardResult;
import in.bachatsetu.backend.shared.domain.AggregateId;

public interface GetMemberDashboardUseCase {

    MemberDashboardResult execute(AggregateId tenantId, AggregateId userId);
}
