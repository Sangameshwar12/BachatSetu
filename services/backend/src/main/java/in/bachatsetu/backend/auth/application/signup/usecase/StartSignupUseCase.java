package in.bachatsetu.backend.auth.application.signup.usecase;

import in.bachatsetu.backend.auth.application.signup.command.StartSignupCommand;
import in.bachatsetu.backend.auth.application.signup.query.SignupStartedResult;

@FunctionalInterface
public interface StartSignupUseCase {

    SignupStartedResult execute(StartSignupCommand command);
}
