package in.bachatsetu.backend.auth.domain.model;

import in.bachatsetu.backend.auth.domain.event.OtpGenerated;
import in.bachatsetu.backend.auth.domain.event.OtpVerified;
import in.bachatsetu.backend.auth.domain.exception.IdentityDomainException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One-time-password verification aggregate. */
public final class OtpVerification extends BaseAggregateRoot {

    private final UserId userId;
    private final OtpCode code;
    private final OtpPurpose purpose;
    private final Instant generatedAt;
    private final Instant expiresAt;
    private OtpStatus status;

    private OtpVerification(
            AggregateId id,
            UserId userId,
            OtpCode code,
            OtpPurpose purpose,
            Instant generatedAt,
            Instant expiresAt,
            OtpStatus status,
            AuditInfo auditInfo) {
        super(id, auditInfo, 0);
        this.userId = Objects.requireNonNull(userId, "user id must not be null");
        this.code = Objects.requireNonNull(code, "OTP code must not be null");
        this.purpose = Objects.requireNonNull(purpose, "OTP purpose must not be null");
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.status = Objects.requireNonNull(status, "OTP status must not be null");
        if (!expiresAt.isAfter(generatedAt)) {
            throw new IllegalArgumentException("OTP expiry must follow generation time");
        }
    }

    public static OtpVerification generate(
            AggregateId id,
            UserId userId,
            OtpCode code,
            OtpPurpose purpose,
            Instant generatedAt,
            Instant expiresAt,
            AggregateId actorId) {
        OtpVerification verification = new OtpVerification(
                id,
                userId,
                code,
                purpose,
                generatedAt,
                expiresAt,
                OtpStatus.PENDING,
                AuditInfo.createdBy(actorId, generatedAt));
        verification.registerEvent(new OtpGenerated(
                UUID.randomUUID(), id, userId, purpose, expiresAt, generatedAt));
        return verification;
    }

    public void verify(OtpCode candidate, AggregateId actorId, Instant verifiedAt) {
        Objects.requireNonNull(candidate, "candidate OTP code must not be null");
        if (status != OtpStatus.PENDING) {
            throw new IdentityDomainException("OTP verification is no longer pending");
        }
        if (!verifiedAt.isBefore(expiresAt)) {
            status = OtpStatus.EXPIRED;
            markChanged(actorId, verifiedAt);
            throw new IdentityDomainException("OTP code has expired");
        }
        if (!code.matches(candidate)) {
            status = OtpStatus.FAILED;
            markChanged(actorId, verifiedAt);
            throw new IdentityDomainException("OTP code does not match");
        }
        status = OtpStatus.VERIFIED;
        markChanged(actorId, verifiedAt);
        registerEvent(new OtpVerified(UUID.randomUUID(), id(), userId, verifiedAt));
    }

    public boolean expire(AggregateId actorId, Instant evaluatedAt) {
        if (status != OtpStatus.PENDING || evaluatedAt.isBefore(expiresAt)) {
            return false;
        }
        status = OtpStatus.EXPIRED;
        markChanged(actorId, evaluatedAt);
        return true;
    }

    public UserId userId() {
        return userId;
    }

    public OtpPurpose purpose() {
        return purpose;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public OtpStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof OtpVerification that && id().equals(that.id());
    }

    @Override
    public int hashCode() {
        return id().hashCode();
    }
}
