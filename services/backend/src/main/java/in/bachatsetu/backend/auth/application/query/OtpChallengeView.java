package in.bachatsetu.backend.auth.application.query;

import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpStatus;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;

/** Safe OTP lifecycle projection containing no credential material. */
public record OtpChallengeView(
        AggregateId verificationId,
        UserId userId,
        OtpPurpose purpose,
        OtpStatus status,
        Instant expiresAt,
        int verificationAttempts,
        int resendCount) {

    public static OtpChallengeView from(OtpVerification verification) {
        return new OtpChallengeView(
                verification.id(),
                verification.userId(),
                verification.purpose(),
                verification.status(),
                verification.expiresAt(),
                verification.verificationAttempts(),
                verification.resendCount());
    }
}
