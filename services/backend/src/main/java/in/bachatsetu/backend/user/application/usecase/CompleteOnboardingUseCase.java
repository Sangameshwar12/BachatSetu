package in.bachatsetu.backend.user.application.usecase;

import in.bachatsetu.backend.user.application.command.CompleteOnboardingCommand;
import in.bachatsetu.backend.user.application.query.OnboardingCompletedResult;

@FunctionalInterface
public interface CompleteOnboardingUseCase {

    OnboardingCompletedResult execute(CompleteOnboardingCommand command);
}
