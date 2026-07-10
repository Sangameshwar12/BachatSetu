package in.bachatsetu.backend.auth.application.signup.usecase;

import in.bachatsetu.backend.auth.application.signup.command.CompleteSignupCommand;
import in.bachatsetu.backend.auth.application.signup.query.SignupCompletedResult;

@FunctionalInterface
public interface CompleteSignupUseCase {

    SignupCompletedResult execute(CompleteSignupCommand command);
}
