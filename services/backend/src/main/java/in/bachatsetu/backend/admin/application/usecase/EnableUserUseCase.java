package in.bachatsetu.backend.admin.application.usecase;

import in.bachatsetu.backend.admin.application.command.EnableUserCommand;
import in.bachatsetu.backend.admin.application.query.PlatformUserResult;

/** Enables one platform user, across any tenant. */
@FunctionalInterface
public interface EnableUserUseCase {

    PlatformUserResult execute(EnableUserCommand command);
}
