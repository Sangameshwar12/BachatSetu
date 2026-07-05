package in.bachatsetu.backend.auth.application.validation;

import in.bachatsetu.backend.auth.application.exception.OtpApplicationException;
import in.bachatsetu.backend.auth.application.exception.OtpFailureReason;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import java.util.Objects;

/** Validates the user prerequisite shared by OTP commands. */
public final class OtpRequestValidator {

    private final UserRepository userRepository;

    public OtpRequestValidator(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "user repository must not be null");
    }

    public User requireUser(UserId userId) {
        Objects.requireNonNull(userId, "user id must not be null");
        return userRepository.findById(userId)
                .orElseThrow(() -> new OtpApplicationException(
                        OtpFailureReason.USER_NOT_FOUND, "authentication user does not exist"));
    }
}
