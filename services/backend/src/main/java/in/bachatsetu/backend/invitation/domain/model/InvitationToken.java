package in.bachatsetu.backend.invitation.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/** Cryptographically random, unguessable token embedded in QR codes and shareable links. */
public record InvitationToken(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9_-]{32,64}$");

    public InvitationToken {
        Objects.requireNonNull(value, "invitation token must not be null");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("invitation token has an invalid format");
        }
    }
}
