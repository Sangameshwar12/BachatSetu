package in.bachatsetu.backend.admin.application.usecase;

import in.bachatsetu.backend.admin.application.query.PlatformStatisticsResult;

/** Computes platform-wide totals on demand. */
@FunctionalInterface
public interface GetPlatformStatisticsUseCase {

    PlatformStatisticsResult execute();
}
