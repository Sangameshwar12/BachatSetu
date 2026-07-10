package in.bachatsetu.backend.admin.application.configuration.usecase;

import in.bachatsetu.backend.admin.application.configuration.command.UpdateSystemLimitsCommand;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformLimitResult;
import java.util.List;

@FunctionalInterface
public interface UpdateSystemLimitsUseCase {

    List<PlatformLimitResult> execute(UpdateSystemLimitsCommand command);
}
