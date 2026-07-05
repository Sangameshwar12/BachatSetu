package in.bachatsetu.backend.auth.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Email;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentityDomainEventTest {

    private static final Instant NOW = Instant.parse("2026-07-05T06:00:00Z");

    @Test
    void eventsExposeOnlyIdentityAndLifecycleMetadata() {
        UserId userId = UserId.newId();
        RefreshTokenId tokenId = RefreshTokenId.newId();
        AggregateId otpId = AggregateId.newId();
        UserRegistered registered = new UserRegistered(
                UUID.randomUUID(),
                userId,
                new Email("member@example.com"),
                MobileNumber.of("+919876543210"),
                NOW);
        PasswordChanged passwordChanged = new PasswordChanged(UUID.randomUUID(), userId, NOW);
        OtpGenerated generated = new OtpGenerated(
                UUID.randomUUID(), otpId, userId, OtpPurpose.SIGN_IN, NOW.plusSeconds(60), NOW);
        OtpVerified verified = new OtpVerified(UUID.randomUUID(), otpId, userId, NOW.plusSeconds(1));
        RefreshTokenCreated created = new RefreshTokenCreated(
                UUID.randomUUID(), tokenId, userId, NOW.plusSeconds(60), NOW);
        RefreshTokenRevoked revoked = new RefreshTokenRevoked(
                UUID.randomUUID(), tokenId, userId, NOW.plusSeconds(1));

        assertThat(registered.aggregateId()).isEqualTo(userId.toAggregateId());
        assertThat(passwordChanged.aggregateId()).isEqualTo(userId.toAggregateId());
        assertThat(generated.aggregateId()).isEqualTo(otpId);
        assertThat(verified.aggregateId()).isEqualTo(otpId);
        assertThat(created.aggregateId()).isEqualTo(tokenId.toAggregateId());
        assertThat(revoked.aggregateId()).isEqualTo(tokenId.toAggregateId());
        assertThat(registered.toString()).doesNotContain("password");
        assertThat(generated.toString()).doesNotContain("123456");
    }

    @Test
    void creationEventsRejectNonFutureExpiry() {
        UserId userId = UserId.newId();

        assertThatIllegalArgumentException().isThrownBy(() -> new OtpGenerated(
                UUID.randomUUID(), AggregateId.newId(), userId, OtpPurpose.SIGN_IN, NOW, NOW));
        assertThatIllegalArgumentException().isThrownBy(() -> new RefreshTokenCreated(
                UUID.randomUUID(), RefreshTokenId.newId(), userId, NOW, NOW));
    }
}
