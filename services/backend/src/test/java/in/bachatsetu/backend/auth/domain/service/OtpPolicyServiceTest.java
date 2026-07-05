package in.bachatsetu.backend.auth.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.auth.domain.model.OtpHash;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpStatus;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OtpPolicyServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-05T06:00:00Z");
    private static final OtpHash HASH = OtpHash.encoded("A".repeat(64));

    private final OtpPolicyService service = new OtpPolicyService();

    @Test
    void appliesFixedFiveMinuteValidityToGenerationAndResend() {
        AggregateId actorId = AggregateId.newId();
        OtpVerification initial = service.generate(
                AggregateId.newId(), UserId.newId(), HASH, OtpPurpose.SIGN_IN, actorId, NOW);
        OtpVerification replacement = service.resend(
                initial,
                AggregateId.newId(),
                OtpHash.encoded("B".repeat(64)),
                actorId,
                NOW.plusSeconds(30));

        assertThat(initial.expiresAt()).isEqualTo(NOW.plus(OtpPolicyService.VALIDITY));
        assertThat(initial.status()).isEqualTo(OtpStatus.INVALIDATED);
        assertThat(replacement.expiresAt())
                .isEqualTo(NOW.plusSeconds(30).plus(OtpPolicyService.VALIDITY));
        assertThat(replacement.resendCount()).isOne();
    }
}
