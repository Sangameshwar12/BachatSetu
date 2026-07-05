package in.bachatsetu.backend.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.domain.event.RefreshTokenCreated;
import in.bachatsetu.backend.auth.domain.event.RefreshTokenRevoked;
import in.bachatsetu.backend.auth.domain.exception.IdentityDomainException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-07-05T06:00:00Z");

    @Test
    void createsUsableTokenAndRevokesItOnce() {
        AggregateId actorId = AggregateId.newId();
        RefreshTokenId tokenId = RefreshTokenId.newId();
        UserId userId = UserId.newId();
        RefreshToken token = RefreshToken.issue(
                tokenId, userId, NOW, NOW.plusSeconds(3600), actorId);

        assertThat(token.refreshTokenId()).isEqualTo(tokenId);
        assertThat(token.userId()).isEqualTo(userId);
        assertThat(token.issuedAt()).isEqualTo(NOW);
        assertThat(token.expiresAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(token.status()).isEqualTo(TokenStatus.ACTIVE);
        assertThat(token.isUsableAt(NOW.plusSeconds(1))).isTrue();
        assertThat(token.isUsableAt(NOW.plusSeconds(3600))).isFalse();
        RefreshTokenCreated created = (RefreshTokenCreated) token.domainEvents().getFirst();
        assertThat(created.aggregateId()).isEqualTo(tokenId.toAggregateId());

        token.revoke(actorId, NOW.plusSeconds(10));

        assertThat(token.status()).isEqualTo(TokenStatus.REVOKED);
        assertThat(token.isUsableAt(NOW.plusSeconds(11))).isFalse();
        assertThat(token.domainEvents().getLast()).isInstanceOf(RefreshTokenRevoked.class);
        RefreshTokenRevoked revoked = (RefreshTokenRevoked) token.domainEvents().getLast();
        assertThat(revoked.aggregateId()).isEqualTo(tokenId.toAggregateId());
        assertThatThrownBy(() -> token.revoke(actorId, NOW.plusSeconds(20)))
                .isInstanceOf(IdentityDomainException.class);
    }

    @Test
    void expiresOnlyAtOrAfterExpiry() {
        AggregateId actorId = AggregateId.newId();
        RefreshToken token = RefreshToken.issue(
                RefreshTokenId.newId(), UserId.newId(), NOW, NOW.plusSeconds(60), actorId);

        assertThat(token.expire(actorId, NOW.plusSeconds(59))).isFalse();
        assertThat(token.expire(actorId, NOW.plusSeconds(60))).isTrue();
        assertThat(token.status()).isEqualTo(TokenStatus.EXPIRED);
        assertThat(token.expire(actorId, NOW.plusSeconds(61))).isFalse();
        assertThatThrownBy(() -> token.revoke(actorId, NOW.plusSeconds(61)))
                .isInstanceOf(IdentityDomainException.class);
    }

    @Test
    void rejectsInvalidExpiryAndUsesTokenIdentityForEquality() {
        AggregateId actorId = AggregateId.newId();
        RefreshTokenId tokenId = RefreshTokenId.newId();
        UserId userId = UserId.newId();
        RefreshToken first = RefreshToken.issue(tokenId, userId, NOW, NOW.plusSeconds(60), actorId);
        RefreshToken sameIdentity = RefreshToken.issue(tokenId, userId, NOW, NOW.plusSeconds(120), actorId);
        RefreshToken different = RefreshToken.issue(
                RefreshTokenId.newId(), userId, NOW, NOW.plusSeconds(60), actorId);

        assertThat(first).isEqualTo(first).isEqualTo(sameIdentity).hasSameHashCodeAs(sameIdentity);
        assertThat(first).isNotEqualTo(different).isNotEqualTo(null).isNotEqualTo("token");
        assertThatIllegalArgumentException().isThrownBy(() -> RefreshToken.issue(
                RefreshTokenId.newId(), userId, NOW, NOW, actorId));
    }

    @Test
    void rehydratesTerminalStateWithoutEvents() {
        AggregateId actorId = AggregateId.newId();
        var auditInfo = in.bachatsetu.backend.shared.domain.AuditInfo.createdBy(actorId, NOW);
        RefreshToken token = RefreshToken.rehydrate(
                RefreshTokenId.newId(),
                UserId.newId(),
                NOW,
                NOW.plusSeconds(60),
                TokenStatus.REVOKED,
                auditInfo,
                5);

        assertThat(token.status()).isEqualTo(TokenStatus.REVOKED);
        assertThat(token.auditInfo()).isEqualTo(auditInfo);
        assertThat(token.version()).isEqualTo(5);
        assertThat(token.domainEvents()).isEmpty();
    }
}
