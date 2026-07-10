package in.bachatsetu.backend.invitation.application.usecase;

import in.bachatsetu.backend.invitation.application.command.RevokeInvitationCommand;

@FunctionalInterface
public interface RevokeInvitationUseCase {

    void execute(RevokeInvitationCommand command);
}
