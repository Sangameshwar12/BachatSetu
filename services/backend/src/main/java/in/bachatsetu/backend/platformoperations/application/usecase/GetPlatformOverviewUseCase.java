package in.bachatsetu.backend.platformoperations.application.usecase;

import in.bachatsetu.backend.platformoperations.application.query.PlatformOverviewResult;

@FunctionalInterface
public interface GetPlatformOverviewUseCase {

    PlatformOverviewResult execute();
}
