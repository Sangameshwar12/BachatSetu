package in.bachatsetu.backend.invitation.application.port;

import in.bachatsetu.backend.invitation.domain.model.InvitationToken;

@FunctionalInterface
public interface InvitationTokenGeneratorPort {

    InvitationToken generate();
}
