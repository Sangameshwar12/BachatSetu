package in.bachatsetu.backend.auth.application.security;

import java.util.Optional;

/** Framework-independent access to the current authenticated identity. */
public interface CurrentUserProvider {

    Optional<AuthenticatedUser> currentUser();

    default AuthenticatedUser requireCurrentUser() {
        return currentUser().orElseThrow(CurrentUserUnavailableException::new);
    }
}
