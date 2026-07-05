package in.bachatsetu.backend.auth.application.token.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Revocation request that redacts its refresh credential from string rendering. */
public final class RevokeRefreshTokenCommand {

    private final String refreshToken;
    private final AggregateId actorId;

    public RevokeRefreshTokenCommand(String refreshToken, AggregateId actorId) {
        this.refreshToken = requireToken(refreshToken);
        this.actorId = Objects.requireNonNull(actorId, "actor id must not be null");
    }

    public String refreshToken() {
        return refreshToken;
    }

    public AggregateId actorId() {
        return actorId;
    }

    private static String requireToken(String value) {
        Objects.requireNonNull(value, "refresh token must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("refresh token must not be blank");
        }
        return value;
    }

    @Override
    public String toString() {
        return "RevokeRefreshTokenCommand[refreshToken=[REDACTED]]";
    }
}
