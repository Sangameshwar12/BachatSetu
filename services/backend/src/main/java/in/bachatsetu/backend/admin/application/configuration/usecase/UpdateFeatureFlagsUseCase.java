package in.bachatsetu.backend.admin.application.configuration.usecase;

import in.bachatsetu.backend.admin.application.configuration.command.UpdateFeatureFlagsCommand;
import in.bachatsetu.backend.admin.application.configuration.query.FeatureFlagResult;
import java.util.List;

@FunctionalInterface
public interface UpdateFeatureFlagsUseCase {

    List<FeatureFlagResult> execute(UpdateFeatureFlagsCommand command);
}
