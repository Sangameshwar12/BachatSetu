package in.bachatsetu.backend.platformoperations.application.usecase;

import in.bachatsetu.backend.platformoperations.application.query.SystemHealthResult;

@FunctionalInterface
public interface GetSystemHealthUseCase {

    SystemHealthResult execute();
}
