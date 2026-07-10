package in.bachatsetu.backend.invitation.interfaces.rest.adapter;

import in.bachatsetu.backend.invitation.application.port.InvitationTokenGeneratorPort;
import in.bachatsetu.backend.invitation.domain.model.InvitationToken;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/** Generates a cryptographically random, unguessable token for QR codes and shareable links. */
public final class RandomInvitationTokenGeneratorAdapter implements InvitationTokenGeneratorPort {

    private static final int RANDOM_BYTES = 32;

    private final SecureRandom secureRandom;

    public RandomInvitationTokenGeneratorAdapter(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
    }

    @Override
    public InvitationToken generate() {
        byte[] bytes = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(bytes);
        return new InvitationToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }
}
