package in.bachatsetu.backend.invitation.interfaces.rest.adapter;

import in.bachatsetu.backend.invitation.application.port.InvitationCodeGeneratorPort;
import in.bachatsetu.backend.invitation.domain.model.InvitationCode;
import java.security.SecureRandom;
import java.util.Objects;

/** Generates a random, unguessable 8-character human-typeable invitation code. */
public final class RandomInvitationCodeGeneratorAdapter implements InvitationCodeGeneratorPort {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;

    private final SecureRandom secureRandom;

    public RandomInvitationCodeGeneratorAdapter(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
    }

    @Override
    public InvitationCode generate() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return new InvitationCode(code.toString());
    }
}
