package in.bachatsetu.backend.auth.domain.port;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Email;
import in.bachatsetu.backend.shared.domain.PhoneNumber;
import java.time.Instant;

/**
 * Provisions the user-profile side of a signup without the auth module depending on the user
 * module's application or domain types directly (the two modules would otherwise form a package
 * cycle: auth already requires this to compose signup, and the user module's onboarding REST layer
 * separately depends on auth for the caller's identity). The adapter implementing this port is
 * free to depend on the user module's domain layer, since general infrastructure may depend on any
 * module's domain package.
 */
public interface ProfileProvisioningPort {

    boolean existsByPhoneNumber(PhoneNumber phoneNumber);

    boolean existsByEmail(Email email);

    void createProfile(
            AggregateId id,
            String givenName,
            String familyName,
            Email email,
            PhoneNumber phoneNumber,
            String preferredLanguage,
            AggregateId actorId,
            Instant createdAt);

    void activateProfile(AggregateId id, AggregateId actorId, Instant activatedAt);
}
