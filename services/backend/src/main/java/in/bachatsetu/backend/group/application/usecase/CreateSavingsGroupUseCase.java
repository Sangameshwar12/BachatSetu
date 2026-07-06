package in.bachatsetu.backend.group.application.usecase;

import in.bachatsetu.backend.group.application.command.CreateSavingsGroupCommand;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;

/** Creates a savings group and returns its current state. */
@FunctionalInterface
public interface CreateSavingsGroupUseCase {

    SavingsGroupResult execute(CreateSavingsGroupCommand command);
}
