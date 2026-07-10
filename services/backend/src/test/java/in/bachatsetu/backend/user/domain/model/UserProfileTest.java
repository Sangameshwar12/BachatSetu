package in.bachatsetu.backend.user.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Email;
import in.bachatsetu.backend.shared.domain.PhoneNumber;
import in.bachatsetu.backend.user.domain.event.ProfileCompleted;
import in.bachatsetu.backend.user.domain.event.UserRegistered;
import in.bachatsetu.backend.user.domain.exception.InvalidUserStateException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserProfileTest {

    private static final Instant NOW = Instant.parse("2026-07-09T06:00:00Z");

    @Test
    void registersAsInvitedAndEmitsUserRegistered() {
        AggregateId id = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        UserProfile profile = register(id, actorId);

        assertThat(profile.status()).isEqualTo(UserStatus.INVITED);
        assertThat(profile.onboarded()).isFalse();
        assertThat(profile.notificationsEnabled()).isTrue();
        assertThat(profile.domainEvents()).singleElement().isInstanceOf(UserRegistered.class);
    }

    @Test
    void completesOnboardingExactlyOnce() {
        AggregateId id = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        UserProfile profile = register(id, actorId);
        AggregateId photoFileId = AggregateId.newId();

        profile.completeOnboarding("Pune", "Maharashtra", photoFileId, false, actorId, NOW.plusSeconds(1));

        assertThat(profile.city()).isEqualTo("Pune");
        assertThat(profile.state()).isEqualTo("Maharashtra");
        assertThat(profile.photoFileId()).isEqualTo(photoFileId);
        assertThat(profile.notificationsEnabled()).isFalse();
        assertThat(profile.onboarded()).isTrue();
        assertThat(profile.domainEvents()).anyMatch(ProfileCompleted.class::isInstance);
        ProfileCompleted event = (ProfileCompleted) profile.domainEvents().getLast();
        assertThat(event.aggregateId()).isEqualTo(id);

        assertThatThrownBy(() -> profile.completeOnboarding(
                        "Mumbai", "Maharashtra", null, true, actorId, NOW.plusSeconds(2)))
                .isInstanceOf(InvalidUserStateException.class);
    }

    @Test
    void activatesAnInvitedProfile() {
        AggregateId id = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        UserProfile profile = register(id, actorId);

        profile.activate(actorId, NOW.plusSeconds(1));

        assertThat(profile.status()).isEqualTo(UserStatus.ACTIVE);
    }

    private UserProfile register(AggregateId id, AggregateId actorId) {
        PersonName name = new PersonName("Asha", "Rao");
        UserContact contact = new UserContact(new Email("asha@example.com"), new PhoneNumber("+919876543210"));
        return UserProfile.register(id, name, contact, PreferredLanguage.ENGLISH, actorId, NOW);
    }
}
