package in.bachatsetu.backend.group.application.usecase;

import in.bachatsetu.backend.group.application.command.JoinGroupCommand;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;

/** Adds a member through the Savings Group aggregate. */
@FunctionalInterface
public interface JoinGroupUseCase {

    SavingsGroupResult execute(JoinGroupCommand command);
}
