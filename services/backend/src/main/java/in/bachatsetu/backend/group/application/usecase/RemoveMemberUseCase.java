package in.bachatsetu.backend.group.application.usecase;

import in.bachatsetu.backend.group.application.command.RemoveMemberCommand;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;

/** Removes a member through the Savings Group aggregate. */
@FunctionalInterface
public interface RemoveMemberUseCase {

    SavingsGroupResult execute(RemoveMemberCommand command);
}
