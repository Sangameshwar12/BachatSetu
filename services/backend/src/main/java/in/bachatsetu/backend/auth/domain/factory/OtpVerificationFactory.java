package in.bachatsetu.backend.auth.domain.factory;

import in.bachatsetu.backend.auth.domain.model.OtpHash;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.domain.service.OtpPolicyService;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Clock;
import java.util.Objects;

/** Creates OTP verifications using externally generated secure OTP values. */
public final class OtpVerificationFactory {

    private final Clock clock;
    private final OtpPolicyService policyService;

    public OtpVerificationFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.policyService = new OtpPolicyService();
    }

    public OtpVerification generate(
            UserId userId,
            OtpHash hash,
            OtpPurpose purpose,
            AggregateId actorId) {
        return policyService.generate(AggregateId.newId(), userId, hash, purpose, actorId, clock.instant());
    }
}
