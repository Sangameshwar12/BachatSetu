package in.bachatsetu.backend.user.domain.port;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Email;
import in.bachatsetu.backend.shared.domain.PhoneNumber;
import in.bachatsetu.backend.user.domain.model.UserProfile;
import java.util.Optional;

public interface UserRepository {

    Optional<UserProfile> findById(AggregateId userId);

    Optional<UserProfile> findByEmail(Email email);

    Optional<UserProfile> findByPhoneNumber(PhoneNumber phoneNumber);

    void save(UserProfile user);
}
