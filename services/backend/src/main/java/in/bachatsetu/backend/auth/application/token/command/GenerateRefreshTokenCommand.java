package in.bachatsetu.backend.auth.application.token.command;

import in.bachatsetu.backend.auth.domain.model.TokenSessionId;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public record GenerateRefreshTokenCommand(
        UserId userId,
        AggregateId tenantId,
        TokenSessionId sessionId,
        AggregateId actorId) {

    public GenerateRefreshTokenCommand {
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(sessionId, "token session id must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
