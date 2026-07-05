package in.bachatsetu.backend.infrastructure.persistence.entity.identity;

import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpStatus;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "otp_verifications",
        schema = "identity",
        indexes = {
            @Index(name = "idx_otp_user_purpose_status", columnList = "user_id,purpose,status"),
            @Index(name = "idx_otp_expires_at", columnList = "expires_at")
        })
public class OtpVerificationJpaEntity extends BaseJpaEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_otp_verifications_user"))
    private UserJpaEntity user;

    @NotBlank
    @Size(min = 32, max = 255)
    @Column(name = "otp_hash", nullable = false, updatable = false, length = 255)
    private String hash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, updatable = false, length = 30)
    private OtpPurpose purpose;

    @NotNull
    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OtpStatus status;

    @Min(0)
    @Max(OtpVerification.MAXIMUM_VERIFICATION_ATTEMPTS)
    @Column(name = "verification_attempts", nullable = false)
    private int verificationAttempts;

    @Min(0)
    @Max(OtpVerification.MAXIMUM_RESENDS)
    @Column(name = "resend_count", nullable = false, updatable = false)
    private int resendCount;

    protected OtpVerificationJpaEntity() {
    }

    public OtpVerificationJpaEntity(
            UUID id,
            UserJpaEntity user,
            String hash,
            OtpPurpose purpose,
            Instant generatedAt,
            Instant expiresAt,
            OtpStatus status,
            int verificationAttempts,
            int resendCount) {
        super(id);
        this.user = user;
        this.hash = hash;
        this.purpose = purpose;
        this.generatedAt = generatedAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.verificationAttempts = verificationAttempts;
        this.resendCount = resendCount;
    }

    public UserJpaEntity getUser() { return user; }
    public String getHash() { return hash; }
    public OtpPurpose getPurpose() { return purpose; }
    public Instant getGeneratedAt() { return generatedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public OtpStatus getStatus() { return status; }
    public int getVerificationAttempts() { return verificationAttempts; }
    public int getResendCount() { return resendCount; }

    public void updateLifecycle(Instant expiry, OtpStatus otpStatus, int attempts) {
        expiresAt = expiry;
        status = otpStatus;
        verificationAttempts = attempts;
    }
}
