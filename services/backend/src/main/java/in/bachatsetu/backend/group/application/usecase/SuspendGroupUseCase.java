package in.bachatsetu.backend.group.application.usecase;

import in.bachatsetu.backend.group.application.command.SuspendGroupCommand;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;

/** Suspends an active Savings Group aggregate. */
@FunctionalInterface
public interface SuspendGroupUseCase {

    SavingsGroupResult execute(SuspendGroupCommand command);
}
