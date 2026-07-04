package in.bachatsetu.backend.auth.domain.model;

import in.bachatsetu.backend.auth.domain.exception.InvalidVerificationChallengeException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

public final class VerificationChallenge {

    private final AggregateId id;
    private final VerificationPurpose purpose;
    private final Instant expiresAt;
    private VerificationStatus status;

    public VerificationChallenge(
            AggregateId id,
            VerificationPurpose purpose,
            Instant expiresAt,
            VerificationStatus status) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.purpose = Objects.requireNonNull(purpose, "purpose must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public void verify(Instant verifiedAt) {
        Objects.requireNonNull(verifiedAt, "verifiedAt must not be null");
        if (status != VerificationStatus.PENDING || !verifiedAt.isBefore(expiresAt)) {
            throw new InvalidVerificationChallengeException("challenge is not verifiable");
        }
        status = VerificationStatus.VERIFIED;
    }

    public void expire(Instant expiredAt) {
        Objects.requireNonNull(expiredAt, "expiredAt must not be null");
        if (status == VerificationStatus.PENDING && !expiredAt.isBefore(expiresAt)) {
            status = VerificationStatus.EXPIRED;
        }
    }

    public AggregateId id() {
        return id;
    }

    public VerificationPurpose purpose() {
        return purpose;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public VerificationStatus status() {
        return status;
    }
}
