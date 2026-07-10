package in.bachatsetu.backend.infrastructure.auth.adapter;

import in.bachatsetu.backend.auth.application.port.PasswordHashGeneratorPort;
import in.bachatsetu.backend.auth.domain.model.PasswordHash;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Generates a random, encoded password nobody ever authenticates with — this platform is OTP-only,
 * but {@link in.bachatsetu.backend.auth.domain.model.User#register} requires a non-null {@code
 * PasswordHash} to satisfy the aggregate's existing invariant.
 */
public final class RandomPasswordHashGeneratorAdapter implements PasswordHashGeneratorPort {

    private static final int RANDOM_BYTES = 32;

    private final BCryptPasswordEncoder encoder;
    private final SecureRandom secureRandom;

    public RandomPasswordHashGeneratorAdapter(BCryptPasswordEncoder encoder, SecureRandom secureRandom) {
        this.encoder = Objects.requireNonNull(encoder, "BCrypt encoder must not be null");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secure random must not be null");
    }

    @Override
    public PasswordHash generateRandom() {
        byte[] bytes = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(bytes);
        String rawSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return PasswordHash.encoded(encoder.encode(rawSecret));
    }
}
