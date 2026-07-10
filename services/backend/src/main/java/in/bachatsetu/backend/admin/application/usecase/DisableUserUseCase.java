package in.bachatsetu.backend.admin.application.usecase;

import in.bachatsetu.backend.admin.application.command.DisableUserCommand;
import in.bachatsetu.backend.admin.application.query.PlatformUserResult;

/** Disables one platform user, across any tenant. */
@FunctionalInterface
public interface DisableUserUseCase {

    PlatformUserResult execute(DisableUserCommand command);
}
