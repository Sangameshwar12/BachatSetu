package in.bachatsetu.backend.auth.application.login.usecase;

import in.bachatsetu.backend.auth.application.login.command.CompleteLoginCommand;
import in.bachatsetu.backend.auth.application.login.query.LoginCompletedResult;

@FunctionalInterface
public interface CompleteLoginUseCase {

    LoginCompletedResult execute(CompleteLoginCommand command);
}
