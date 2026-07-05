package in.bachatsetu.backend.auth.application.token.command;

import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public record GenerateAccessTokenCommand(UserId userId, AggregateId tenantId) {

    public GenerateAccessTokenCommand {
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(tenantId, "tenant id must not be null");
    }
}
