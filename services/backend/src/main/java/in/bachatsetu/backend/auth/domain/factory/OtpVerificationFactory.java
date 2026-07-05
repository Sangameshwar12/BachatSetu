package in.bachatsetu.backend.auth.domain.factory;

import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Creates OTP verifications using externally generated secure OTP values. */
public final class OtpVerificationFactory {

    private final Clock clock;
    private final Duration lifetime;

    public OtpVerificationFactory(Clock clock, Duration lifetime) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.lifetime = requirePositive(lifetime);
    }

    public OtpVerification generate(
            UserId userId,
            OtpCode code,
            OtpPurpose purpose,
            AggregateId actorId) {
        Instant generatedAt = clock.instant();
        return OtpVerification.generate(
                AggregateId.newId(),
                userId,
                code,
                purpose,
                generatedAt,
                generatedAt.plus(lifetime),
                actorId);
    }

    private static Duration requirePositive(Duration value) {
        Objects.requireNonNull(value, "OTP lifetime must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("OTP lifetime must be positive");
        }
        return value;
    }
}
