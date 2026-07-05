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
    public static final int MAXIMUM_VERIFICATION_ATTEMPTS = 5;
    public static final int MAXIMUM_RESENDS = 3;

    private final OtpHash hash;
    private final OtpPurpose purpose;
    private final Instant generatedAt;
    private final Instant expiresAt;
    private OtpStatus status;
    private int verificationAttempts;
    private final int resendCount;

    private OtpVerification(
            AggregateId id,
            UserId userId,
            OtpHash hash,
            OtpPurpose purpose,
            Instant generatedAt,
            Instant expiresAt,
            OtpStatus status,
            int verificationAttempts,
            int resendCount,
            AuditInfo auditInfo,
            long version) {
        super(id, auditInfo, version);
        this.userId = Objects.requireNonNull(userId, "user id must not be null");
        this.hash = Objects.requireNonNull(hash, "OTP hash must not be null");
        this.purpose = Objects.requireNonNull(purpose, "OTP purpose must not be null");
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.status = Objects.requireNonNull(status, "OTP status must not be null");
        this.verificationAttempts = requireRange(
                verificationAttempts, 0, MAXIMUM_VERIFICATION_ATTEMPTS, "verification attempts");
        this.resendCount = requireRange(resendCount, 0, MAXIMUM_RESENDS, "resend count");
        if (!expiresAt.isAfter(generatedAt)) {
            throw new IllegalArgumentException("OTP expiry must follow generation time");
        }
    }

    public static OtpVerification generate(
            AggregateId id,
            UserId userId,
            OtpHash hash,
            OtpPurpose purpose,
            Instant generatedAt,
            Instant expiresAt,
            AggregateId actorId) {
        OtpVerification verification = new OtpVerification(
                id,
                userId,
                hash,
                purpose,
                generatedAt,
                expiresAt,
                OtpStatus.PENDING,
                0,
                0,
                AuditInfo.createdBy(actorId, generatedAt),
                0);
        verification.registerEvent(new OtpGenerated(
                UUID.randomUUID(), id, userId, purpose, expiresAt, generatedAt));
        return verification;
    }

    /** Reconstructs persisted OTP state without emitting domain events. */
    public static OtpVerification rehydrate(
            AggregateId id,
            UserId userId,
            OtpHash hash,
            OtpPurpose purpose,
            Instant generatedAt,
            Instant expiresAt,
            OtpStatus status,
            int verificationAttempts,
            int resendCount,
            AuditInfo auditInfo,
            long version) {
        return new OtpVerification(
                id,
                userId,
                hash,
                purpose,
                generatedAt,
                expiresAt,
                status,
                verificationAttempts,
                resendCount,
                auditInfo,
                version);
    }

    public boolean verify(boolean hashMatches, AggregateId actorId, Instant verifiedAt) {
        if (status != OtpStatus.PENDING) {
            throw new IdentityDomainException("OTP verification is no longer pending");
        }
        if (!verifiedAt.isBefore(expiresAt)) {
            status = OtpStatus.EXPIRED;
            markChanged(actorId, verifiedAt);
            return false;
        }
        verificationAttempts++;
        if (!hashMatches) {
            if (verificationAttempts == MAXIMUM_VERIFICATION_ATTEMPTS) {
                status = OtpStatus.FAILED;
            }
            markChanged(actorId, verifiedAt);
            return false;
        }
        status = OtpStatus.VERIFIED;
        markChanged(actorId, verifiedAt);
        registerEvent(new OtpVerified(UUID.randomUUID(), id(), userId, verifiedAt));
        return true;
    }

    public boolean expire(AggregateId actorId, Instant evaluatedAt) {
        if (status != OtpStatus.PENDING || evaluatedAt.isBefore(expiresAt)) {
            return false;
        }
        status = OtpStatus.EXPIRED;
        markChanged(actorId, evaluatedAt);
        return true;
    }

    public boolean invalidate(AggregateId actorId, Instant invalidatedAt) {
        if (status != OtpStatus.PENDING) {
            return false;
        }
        status = OtpStatus.INVALIDATED;
        markChanged(actorId, invalidatedAt);
        return true;
    }

    public OtpVerification resendAs(
            AggregateId replacementId,
            OtpHash replacementHash,
            Instant generatedAt,
            Instant replacementExpiresAt,
            AggregateId actorId) {
        if (status != OtpStatus.PENDING) {
            throw new IdentityDomainException("only a pending OTP can be resent");
        }
        if (resendCount >= MAXIMUM_RESENDS) {
            throw new IdentityDomainException("maximum OTP resend count reached");
        }
        status = generatedAt.isBefore(expiresAt) ? OtpStatus.INVALIDATED : OtpStatus.EXPIRED;
        markChanged(actorId, generatedAt);
        OtpVerification replacement = new OtpVerification(
                replacementId,
                userId,
                replacementHash,
                purpose,
                generatedAt,
                replacementExpiresAt,
                OtpStatus.PENDING,
                0,
                resendCount + 1,
                AuditInfo.createdBy(actorId, generatedAt),
                0);
        replacement.registerEvent(new OtpGenerated(
                UUID.randomUUID(), replacementId, userId, purpose, replacementExpiresAt, generatedAt));
        return replacement;
    }

    public UserId userId() {
        return userId;
    }

    public OtpHash hash() {
        return hash;
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

    public int verificationAttempts() {
        return verificationAttempts;
    }

    public int resendCount() {
        return resendCount;
    }

    private static int requireRange(int value, int minimum, int maximum, String field) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(field + " must be between " + minimum + " and " + maximum);
        }
        return value;
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
