package in.bachatsetu.backend.auth.domain.port;

import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.Email;
import java.util.Optional;

/** Persistence port for authentication users. */
public interface UserRepository {

    Optional<User> findById(UserId userId);

    Optional<User> findByEmail(Email email);

    Optional<User> findByMobileNumber(MobileNumber mobileNumber);

    void save(User user);
}
