package in.bachatsetu.backend.dashboard.application.usecase;

import in.bachatsetu.backend.dashboard.application.query.OrganizerDashboardResult;
import in.bachatsetu.backend.shared.domain.AggregateId;

public interface GetOrganizerDashboardUseCase {

    OrganizerDashboardResult execute(AggregateId tenantId, AggregateId organizerId);
}
