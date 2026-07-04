package in.bachatsetu.backend.auth.domain.factory;

import in.bachatsetu.backend.auth.domain.model.AuthAccount;
import in.bachatsetu.backend.auth.domain.model.LoginIdentifier;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Clock;
import java.util.Objects;

public final class AuthAccountFactory {

    private final Clock clock;

    public AuthAccountFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public AuthAccount create(
            AggregateId userId,
            LoginIdentifier loginIdentifier,
            AggregateId actorId) {
        return AuthAccount.create(
                AggregateId.newId(), userId, loginIdentifier, actorId, clock.instant());
    }
}
