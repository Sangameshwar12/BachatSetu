package in.bachatsetu.backend.group.application.usecase;

import in.bachatsetu.backend.group.application.command.CloseGroupCommand;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;

/** Permanently closes an eligible Savings Group aggregate. */
@FunctionalInterface
public interface CloseGroupUseCase {

    SavingsGroupResult execute(CloseGroupCommand command);
}
