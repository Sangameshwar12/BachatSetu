package in.bachatsetu.backend.invitation.application.usecase;

import in.bachatsetu.backend.invitation.application.query.InvitationPreviewResult;

@FunctionalInterface
public interface PreviewInvitationUseCase {

    InvitationPreviewResult execute(String token);
}
