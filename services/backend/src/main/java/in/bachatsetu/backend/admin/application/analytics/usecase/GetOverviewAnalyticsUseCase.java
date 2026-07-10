package in.bachatsetu.backend.admin.application.analytics.usecase;

import in.bachatsetu.backend.admin.application.analytics.command.ViewAnalyticsCommand;
import in.bachatsetu.backend.admin.application.analytics.query.OverviewAnalyticsResult;

/** Computes the platform-wide overview snapshot. */
@FunctionalInterface
public interface GetOverviewAnalyticsUseCase {

    OverviewAnalyticsResult execute(ViewAnalyticsCommand command);
}
