package in.bachatsetu.backend.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.domain.event.OtpGenerated;
import in.bachatsetu.backend.auth.domain.event.OtpVerified;
import in.bachatsetu.backend.auth.domain.exception.IdentityDomainException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OtpVerificationTest {

    private static final Instant NOW = Instant.parse("2026-07-05T06:00:00Z");
    private static final OtpHash HASH = OtpHash.encoded("A".repeat(64));
    private static final OtpHash REPLACEMENT_HASH = OtpHash.encoded("B".repeat(64));

    @Test
    void generatesAndVerifiesMatchingHashOnce() {
        AggregateId id = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        UserId userId = UserId.newId();
        OtpVerification verification = OtpVerification.generate(
                id, userId, HASH, OtpPurpose.REGISTRATION, NOW, NOW.plusSeconds(300), actorId);

        assertThat(verification.userId()).isEqualTo(userId);
        assertThat(verification.hash()).isEqualTo(HASH);
        assertThat(verification.purpose()).isEqualTo(OtpPurpose.REGISTRATION);
        assertThat(verification.generatedAt()).isEqualTo(NOW);
        assertThat(verification.expiresAt()).isEqualTo(NOW.plusSeconds(300));
        assertThat(verification.status()).isEqualTo(OtpStatus.PENDING);
        assertThat(verification.verificationAttempts()).isZero();
        assertThat(verification.resendCount()).isZero();
        assertThat(verification.domainEvents()).singleElement().isInstanceOf(OtpGenerated.class);

        assertThat(verification.verify(true, actorId, NOW.plusSeconds(30))).isTrue();

        assertThat(verification.status()).isEqualTo(OtpStatus.VERIFIED);
        assertThat(verification.verificationAttempts()).isOne();
        assertThat(verification.domainEvents().getLast()).isInstanceOf(OtpVerified.class);
        assertThatThrownBy(() -> verification.verify(true, actorId, NOW.plusSeconds(40)))
                .isInstanceOf(IdentityDomainException.class);
    }

    @Test
    void rejectsFiveMismatchesBeforeBecomingTerminal() {
        AggregateId actorId = AggregateId.newId();
        OtpVerification verification = newVerification(actorId);

        for (int attempt = 1; attempt < OtpVerification.MAXIMUM_VERIFICATION_ATTEMPTS; attempt++) {
            assertThat(verification.verify(false, actorId, NOW.plusSeconds(attempt))).isFalse();
            assertThat(verification.status()).isEqualTo(OtpStatus.PENDING);
        }

        assertThat(verification.verify(false, actorId, NOW.plusSeconds(5))).isFalse();
        assertThat(verification.status()).isEqualTo(OtpStatus.FAILED);
        assertThat(verification.verificationAttempts())
                .isEqualTo(OtpVerification.MAXIMUM_VERIFICATION_ATTEMPTS);
    }

    @Test
    void expiresAtBoundaryAndSupportsExplicitInvalidation() {
        AggregateId actorId = AggregateId.newId();
        OtpVerification expiredDuringVerification = newVerification(actorId);

        assertThat(expiredDuringVerification.verify(true, actorId, NOW.plusSeconds(300))).isFalse();
        assertThat(expiredDuringVerification.status()).isEqualTo(OtpStatus.EXPIRED);

        OtpVerification explicitlyExpired = newVerification(actorId);
        assertThat(explicitlyExpired.expire(actorId, NOW.plusSeconds(299))).isFalse();
        assertThat(explicitlyExpired.expire(actorId, NOW.plusSeconds(300))).isTrue();
        assertThat(explicitlyExpired.expire(actorId, NOW.plusSeconds(301))).isFalse();

        OtpVerification invalidated = newVerification(actorId);
        assertThat(invalidated.invalidate(actorId, NOW.plusSeconds(1))).isTrue();
        assertThat(invalidated.status()).isEqualTo(OtpStatus.INVALIDATED);
        assertThat(invalidated.invalidate(actorId, NOW.plusSeconds(2))).isFalse();
    }

    @Test
    void resendsBeforeAndAfterExpiryAndEnforcesLimit() {
        AggregateId actorId = AggregateId.newId();
        OtpVerification current = newVerification(actorId);
        OtpVerification replacement = current.resendAs(
                AggregateId.newId(),
                REPLACEMENT_HASH,
                NOW.plusSeconds(30),
                NOW.plusSeconds(330),
                actorId);

        assertThat(current.status()).isEqualTo(OtpStatus.INVALIDATED);
        assertThat(replacement.hash()).isEqualTo(REPLACEMENT_HASH);
        assertThat(replacement.resendCount()).isOne();
        assertThat(replacement.verificationAttempts()).isZero();

        OtpVerification expiredCurrent = newVerification(actorId);
        OtpVerification afterExpiry = expiredCurrent.resendAs(
                AggregateId.newId(),
                REPLACEMENT_HASH,
                NOW.plusSeconds(300),
                NOW.plusSeconds(600),
                actorId);
        assertThat(expiredCurrent.status()).isEqualTo(OtpStatus.EXPIRED);
        assertThat(afterExpiry.resendCount()).isOne();

        OtpVerification limitReached = rehydrated(OtpStatus.PENDING, 0, 3);
        assertThatThrownBy(() -> limitReached.resendAs(
                        AggregateId.newId(), REPLACEMENT_HASH, NOW.plusSeconds(1), NOW.plusSeconds(301), actorId))
                .isInstanceOf(IdentityDomainException.class);
        OtpVerification terminal = rehydrated(OtpStatus.VERIFIED, 1, 0);
        assertThatThrownBy(() -> terminal.resendAs(
                        AggregateId.newId(), REPLACEMENT_HASH, NOW.plusSeconds(1), NOW.plusSeconds(301), actorId))
                .isInstanceOf(IdentityDomainException.class);
    }

    @Test
    void rehydratesCompleteStateWithoutEventsAndValidatesRanges() {
        OtpVerification verification = rehydrated(OtpStatus.VERIFIED, 2, 1);

        assertThat(verification.status()).isEqualTo(OtpStatus.VERIFIED);
        assertThat(verification.verificationAttempts()).isEqualTo(2);
        assertThat(verification.resendCount()).isOne();
        assertThat(verification.version()).isEqualTo(2);
        assertThat(verification.domainEvents()).isEmpty();
        assertThatIllegalArgumentException().isThrownBy(() -> rehydrated(OtpStatus.PENDING, 6, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> rehydrated(OtpStatus.PENDING, 0, 4));
        assertThatIllegalArgumentException().isThrownBy(() -> OtpVerification.generate(
                AggregateId.newId(),
                UserId.newId(),
                HASH,
                OtpPurpose.SIGN_IN,
                NOW,
                NOW,
                AggregateId.newId()));
    }

    @Test
    void equalityUsesAggregateIdentity() {
        AggregateId actorId = AggregateId.newId();
        AggregateId id = AggregateId.newId();
        UserId userId = UserId.newId();
        OtpVerification first = OtpVerification.generate(
                id, userId, HASH, OtpPurpose.SIGN_IN, NOW, NOW.plusSeconds(60), actorId);
        OtpVerification sameIdentity = OtpVerification.generate(
                id, userId, REPLACEMENT_HASH, OtpPurpose.SIGN_IN, NOW, NOW.plusSeconds(60), actorId);
        OtpVerification different = OtpVerification.generate(
                AggregateId.newId(), userId, HASH, OtpPurpose.SIGN_IN, NOW, NOW.plusSeconds(60), actorId);

        assertThat(first).isEqualTo(first).isEqualTo(sameIdentity).hasSameHashCodeAs(sameIdentity);
        assertThat(first).isNotEqualTo(different).isNotEqualTo(null).isNotEqualTo("OTP");
    }

    private OtpVerification newVerification(AggregateId actorId) {
        return OtpVerification.generate(
                AggregateId.newId(),
                UserId.newId(),
                HASH,
                OtpPurpose.SIGN_IN,
                NOW,
                NOW.plusSeconds(300),
                actorId);
    }

    private OtpVerification rehydrated(OtpStatus status, int attempts, int resends) {
        AggregateId actorId = AggregateId.newId();
        return OtpVerification.rehydrate(
                AggregateId.newId(),
                UserId.newId(),
                HASH,
                OtpPurpose.PASSWORD_RESET,
                NOW,
                NOW.plusSeconds(300),
                status,
                attempts,
                resends,
                AuditInfo.createdBy(actorId, NOW),
                2);
    }
}
