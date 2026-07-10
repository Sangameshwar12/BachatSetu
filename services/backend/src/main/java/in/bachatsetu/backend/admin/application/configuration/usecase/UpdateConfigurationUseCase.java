package in.bachatsetu.backend.admin.application.configuration.usecase;

import in.bachatsetu.backend.admin.application.configuration.command.UpdateConfigurationCommand;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformConfigurationResult;

@FunctionalInterface
public interface UpdateConfigurationUseCase {

    PlatformConfigurationResult execute(UpdateConfigurationCommand command);
}
