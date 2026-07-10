package in.bachatsetu.backend.invitation.application.port;

import in.bachatsetu.backend.invitation.domain.model.InvitationCode;

@FunctionalInterface
public interface InvitationCodeGeneratorPort {

    InvitationCode generate();
}
