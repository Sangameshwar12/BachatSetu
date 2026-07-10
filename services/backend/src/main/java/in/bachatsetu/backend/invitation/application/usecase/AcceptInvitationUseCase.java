package in.bachatsetu.backend.invitation.application.usecase;

import in.bachatsetu.backend.invitation.application.command.AcceptInvitationCommand;
import in.bachatsetu.backend.invitation.application.query.InvitationAcceptedResult;

@FunctionalInterface
public interface AcceptInvitationUseCase {

    InvitationAcceptedResult execute(AcceptInvitationCommand command);
}
