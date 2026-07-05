package in.bachatsetu.backend.auth.domain.factory;

import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.PasswordHash;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Email;
import java.time.Clock;
import java.util.Objects;

/** Creates authentication users with generated identifiers and an injected clock. */
public final class UserFactory {

    private final Clock clock;

    public UserFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public User register(
            Email email,
            MobileNumber mobileNumber,
            PasswordHash passwordHash,
            AggregateId actorId) {
        return User.register(
                UserId.newId(), email, mobileNumber, passwordHash, actorId, clock.instant());
    }
}
