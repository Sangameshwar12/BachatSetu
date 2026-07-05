package in.bachatsetu.backend.auth.domain.service;

import in.bachatsetu.backend.auth.domain.model.OtpHash;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Domain policy for fixed OTP validity and replacement behavior. */
public final class OtpPolicyService {

    public static final Duration VALIDITY = Duration.ofMinutes(5);

    public OtpVerification generate(
            AggregateId verificationId,
            UserId userId,
            OtpHash hash,
            OtpPurpose purpose,
            AggregateId actorId,
            Instant generatedAt) {
        Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        return OtpVerification.generate(
                verificationId, userId, hash, purpose, generatedAt, generatedAt.plus(VALIDITY), actorId);
    }

    public OtpVerification resend(
            OtpVerification current,
            AggregateId replacementId,
            OtpHash replacementHash,
            AggregateId actorId,
            Instant generatedAt) {
        Objects.requireNonNull(current, "current OTP verification must not be null");
        Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        return current.resendAs(
                replacementId, replacementHash, generatedAt, generatedAt.plus(VALIDITY), actorId);
    }
}
