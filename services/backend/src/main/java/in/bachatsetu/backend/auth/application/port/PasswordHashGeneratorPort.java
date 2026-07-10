package in.bachatsetu.backend.auth.application.port;

import in.bachatsetu.backend.auth.domain.model.PasswordHash;

/**
 * Generates a random, encoded password hash for a self-registered user. This platform is
 * OTP-only: nobody ever authenticates with this password, but {@link
 * in.bachatsetu.backend.auth.domain.model.User#register} requires a non-null {@code PasswordHash}
 * to satisfy the aggregate's existing invariant, so signup must supply one.
 */
@FunctionalInterface
public interface PasswordHashGeneratorPort {

    PasswordHash generateRandom();
}
