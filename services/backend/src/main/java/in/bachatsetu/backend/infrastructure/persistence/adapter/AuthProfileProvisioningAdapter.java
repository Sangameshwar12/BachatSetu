package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.auth.domain.port.ProfileProvisioningPort;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Email;
import in.bachatsetu.backend.shared.domain.PhoneNumber;
import in.bachatsetu.backend.user.domain.model.PersonName;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import in.bachatsetu.backend.user.domain.model.UserContact;
import in.bachatsetu.backend.user.domain.model.UserProfile;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Bridges the auth-owned {@link ProfileProvisioningPort} onto the user module's own repository and
 * domain model. Living in general infrastructure (rather than {@code infrastructure.auth}) is what
 * allows this class to depend on the user module's domain layer: general infrastructure may depend
 * on any module's domain package, but the auth module itself must not — see {@link
 * in.bachatsetu.backend.auth.application.signup.service.StartSignupApplicationService} for the full
 * rationale.
 */
@Component
@ConditionalOnPersistenceRepositories
@Profile("local")
public class AuthProfileProvisioningAdapter implements ProfileProvisioningPort {

    private final UserRepository userProfileRepository;

    public AuthProfileProvisioningAdapter(UserRepository userProfileRepository) {
        this.userProfileRepository = Objects.requireNonNull(
                userProfileRepository, "userProfileRepository must not be null");
    }

    @Override
    public boolean existsByPhoneNumber(PhoneNumber phoneNumber) {
        return userProfileRepository.findByPhoneNumber(phoneNumber).isPresent();
    }

    @Override
    public boolean existsByEmail(Email email) {
        return userProfileRepository.findByEmail(email).isPresent();
    }

    @Override
    public void createProfile(
            AggregateId id,
            String givenName,
            String familyName,
            Email email,
            PhoneNumber phoneNumber,
            String preferredLanguage,
            AggregateId actorId,
            Instant createdAt) {
        PersonName name = new PersonName(givenName, familyName);
        UserContact contact = new UserContact(email, phoneNumber);
        PreferredLanguage language = PreferredLanguage.valueOf(preferredLanguage);
        UserProfile profile = UserProfile.register(id, name, contact, language, actorId, createdAt);
        userProfileRepository.save(profile);
    }

    @Override
    public void activateProfile(AggregateId id, AggregateId actorId, Instant activatedAt) {
        UserProfile profile = userProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("profile must exist once the account exists"));
        profile.activate(actorId, activatedAt);
        userProfileRepository.save(profile);
    }
}
