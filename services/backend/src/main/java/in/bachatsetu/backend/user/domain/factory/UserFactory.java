package in.bachatsetu.backend.user.domain.factory;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.user.domain.model.PersonName;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import in.bachatsetu.backend.user.domain.model.UserContact;
import in.bachatsetu.backend.user.domain.model.UserProfile;
import java.time.Clock;
import java.util.Objects;

public final class UserFactory {

    private final Clock clock;

    public UserFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public UserProfile register(
            PersonName name,
            UserContact contact,
            PreferredLanguage preferredLanguage,
            AggregateId actorId) {
        return UserProfile.register(
                AggregateId.newId(), name, contact, preferredLanguage, actorId, clock.instant());
    }
}
