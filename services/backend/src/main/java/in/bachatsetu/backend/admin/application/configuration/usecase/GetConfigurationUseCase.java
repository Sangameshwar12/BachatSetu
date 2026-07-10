package in.bachatsetu.backend.admin.application.configuration.usecase;

import in.bachatsetu.backend.admin.application.configuration.query.PlatformConfigurationResult;

@FunctionalInterface
public interface GetConfigurationUseCase {

    PlatformConfigurationResult execute();
}
