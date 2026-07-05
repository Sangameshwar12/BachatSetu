package in.bachatsetu.backend.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.domain.event.OtpGenerated;
import in.bachatsetu.backend.auth.domain.event.OtpVerified;
import in.bachatsetu.backend.auth.domain.exception.IdentityDomainException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OtpVerificationTest {

    private static final Instant NOW = Instant.parse("2026-07-05T06:00:00Z");

    @Test
    void generatesAndVerifiesUnexpiredMatchingCode() {
        AggregateId id = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        UserId userId = UserId.newId();
        OtpVerification verification = OtpVerification.generate(
                id,
                userId,
                OtpCode.of("123456"),
                OtpPurpose.REGISTRATION,
                NOW,
                NOW.plusSeconds(300),
                actorId);

        assertThat(verification.userId()).isEqualTo(userId);
        assertThat(verification.purpose()).isEqualTo(OtpPurpose.REGISTRATION);
        assertThat(verification.generatedAt()).isEqualTo(NOW);
        assertThat(verification.expiresAt()).isEqualTo(NOW.plusSeconds(300));
        assertThat(verification.status()).isEqualTo(OtpStatus.PENDING);
        OtpGenerated generated = (OtpGenerated) verification.domainEvents().getFirst();
        assertThat(generated.aggregateId()).isEqualTo(id);

        verification.verify(OtpCode.of("123456"), actorId, NOW.plusSeconds(30));

        assertThat(verification.status()).isEqualTo(OtpStatus.VERIFIED);
        assertThat(verification.domainEvents().getLast()).isInstanceOf(OtpVerified.class);
        assertThatThrownBy(() -> verification.verify(
                        OtpCode.of("123456"), actorId, NOW.plusSeconds(40)))
                .isInstanceOf(IdentityDomainException.class);
    }

    @Test
    void failsMismatchedCode() {
        AggregateId actorId = AggregateId.newId();
        OtpVerification verification = newVerification(actorId);

        assertThatThrownBy(() -> verification.verify(
                        OtpCode.of("654321"), actorId, NOW.plusSeconds(1)))
                .isInstanceOf(IdentityDomainException.class);
        assertThat(verification.status()).isEqualTo(OtpStatus.FAILED);
    }

    @Test
    void expiresDuringVerificationAndExplicitEvaluation() {
        AggregateId actorId = AggregateId.newId();
        OtpVerification expiredDuringVerification = newVerification(actorId);
        assertThatThrownBy(() -> expiredDuringVerification.verify(
                        OtpCode.of("123456"), actorId, NOW.plusSeconds(300)))
                .isInstanceOf(IdentityDomainException.class);
        assertThat(expiredDuringVerification.status()).isEqualTo(OtpStatus.EXPIRED);

        OtpVerification explicitlyExpired = newVerification(actorId);
        assertThat(explicitlyExpired.expire(actorId, NOW.plusSeconds(299))).isFalse();
        assertThat(explicitlyExpired.expire(actorId, NOW.plusSeconds(300))).isTrue();
        assertThat(explicitlyExpired.expire(actorId, NOW.plusSeconds(301))).isFalse();
    }

    @Test
    void rejectsInvalidExpiryAndUsesAggregateIdentityForEquality() {
        AggregateId actorId = AggregateId.newId();
        AggregateId id = AggregateId.newId();
        UserId userId = UserId.newId();
        OtpVerification first = OtpVerification.generate(
                id, userId, OtpCode.of("123456"), OtpPurpose.SIGN_IN, NOW, NOW.plusSeconds(60), actorId);
        OtpVerification sameIdentity = OtpVerification.generate(
                id, userId, OtpCode.of("654321"), OtpPurpose.SIGN_IN, NOW, NOW.plusSeconds(60), actorId);
        OtpVerification different = OtpVerification.generate(
                AggregateId.newId(), userId, OtpCode.of("123456"), OtpPurpose.SIGN_IN, NOW, NOW.plusSeconds(60), actorId);

        assertThat(first).isEqualTo(first).isEqualTo(sameIdentity).hasSameHashCodeAs(sameIdentity);
        assertThat(first).isNotEqualTo(different).isNotEqualTo(null).isNotEqualTo("OTP");
        assertThatIllegalArgumentException().isThrownBy(() -> OtpVerification.generate(
                AggregateId.newId(), userId, OtpCode.of("123456"), OtpPurpose.SIGN_IN, NOW, NOW, actorId));
    }

    private OtpVerification newVerification(AggregateId actorId) {
        return OtpVerification.generate(
                AggregateId.newId(),
                UserId.newId(),
                OtpCode.of("123456"),
                OtpPurpose.SIGN_IN,
                NOW,
                NOW.plusSeconds(300),
                actorId);
    }
}
