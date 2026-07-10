package in.bachatsetu.backend.invitation.application.usecase;

import in.bachatsetu.backend.invitation.application.command.CreateInvitationCommand;
import in.bachatsetu.backend.invitation.application.query.InvitationResult;

@FunctionalInterface
public interface CreateInvitationUseCase {

    InvitationResult execute(CreateInvitationCommand command);
}
