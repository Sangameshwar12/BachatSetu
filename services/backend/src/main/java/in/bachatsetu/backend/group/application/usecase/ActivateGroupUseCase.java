package in.bachatsetu.backend.group.application.usecase;

import in.bachatsetu.backend.group.application.command.ActivateGroupCommand;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;

/** Activates an inactive or suspended Savings Group aggregate. */
@FunctionalInterface
public interface ActivateGroupUseCase {

    SavingsGroupResult execute(ActivateGroupCommand command);
}
