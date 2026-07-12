package in.bachatsetu.backend.auth.application.login.usecase;

import in.bachatsetu.backend.auth.application.login.command.StartLoginCommand;
import in.bachatsetu.backend.auth.application.login.query.LoginStartedResult;

@FunctionalInterface
public interface StartLoginUseCase {

    LoginStartedResult execute(StartLoginCommand command);
}
