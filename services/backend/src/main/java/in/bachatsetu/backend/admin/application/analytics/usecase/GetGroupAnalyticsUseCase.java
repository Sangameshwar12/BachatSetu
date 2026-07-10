package in.bachatsetu.backend.admin.application.analytics.usecase;

import in.bachatsetu.backend.admin.application.analytics.command.ViewAnalyticsCommand;
import in.bachatsetu.backend.admin.application.analytics.query.GroupAnalyticsResult;

/** Computes savings group analytics. */
@FunctionalInterface
public interface GetGroupAnalyticsUseCase {

    GroupAnalyticsResult execute(ViewAnalyticsCommand command);
}
