package in.bachatsetu.backend.admin.application.analytics.usecase;

import in.bachatsetu.backend.admin.application.analytics.command.ViewAnalyticsCommand;
import in.bachatsetu.backend.admin.application.analytics.query.StorageAnalyticsResult;

/** Computes storage analytics. */
@FunctionalInterface
public interface GetStorageAnalyticsUseCase {

    StorageAnalyticsResult execute(ViewAnalyticsCommand command);
}
