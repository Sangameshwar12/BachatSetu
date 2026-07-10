package in.bachatsetu.backend.admin.application.analytics.usecase;

import in.bachatsetu.backend.admin.application.analytics.command.ViewAnalyticsCommand;
import in.bachatsetu.backend.admin.application.analytics.query.NotificationAnalyticsResult;

/** Computes notification analytics. */
@FunctionalInterface
public interface GetNotificationAnalyticsUseCase {

    NotificationAnalyticsResult execute(ViewAnalyticsCommand command);
}
