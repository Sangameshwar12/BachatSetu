package in.bachatsetu.backend.admin.application.analytics.usecase;

import in.bachatsetu.backend.admin.application.analytics.command.ViewAnalyticsCommand;
import in.bachatsetu.backend.admin.application.analytics.query.UserAnalyticsResult;

/** Computes platform user analytics. */
@FunctionalInterface
public interface GetUserAnalyticsUseCase {

    UserAnalyticsResult execute(ViewAnalyticsCommand command);
}
