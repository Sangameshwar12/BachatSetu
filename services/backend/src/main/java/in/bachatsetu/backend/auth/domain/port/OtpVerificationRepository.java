package in.bachatsetu.backend.auth.domain.port;

import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;

/** Persistence port for OTP verification records. */
public interface OtpVerificationRepository {

    Optional<OtpVerification> findById(AggregateId verificationId);

    void save(OtpVerification verification);
}
