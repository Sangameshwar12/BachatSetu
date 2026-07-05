package in.bachatsetu.backend.auth.domain.port;

import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;

/** Persistence port for OTP verification records. */
public interface OtpVerificationRepository {

    Optional<OtpVerification> findById(AggregateId verificationId);

    Optional<OtpVerification> findActive(UserId userId, OtpPurpose purpose);

    void save(OtpVerification verification);

    void replace(OtpVerification current, OtpVerification replacement);
}
