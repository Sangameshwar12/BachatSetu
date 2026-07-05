package in.bachatsetu.backend.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.domain.event.RefreshTokenCreated;
import in.bachatsetu.backend.auth.domain.event.RefreshTokenRevoked;
import in.bachatsetu.backend.auth.domain.exception.IdentityDomainException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-07-05T06:00:00Z");
    private static final AggregateId ACTOR_ID = AggregateId.newId();
    private static final AggregateId TENANT_ID = AggregateId.newId();
    private static final TokenSessionId SESSION_ID = TokenSessionId.newId();
    private static final RefreshTokenHash HASH = RefreshTokenHash.encoded("H".repeat(60));

    @Test
    void createsUsableTokenAndRevokesItOnce() {
        RefreshTokenId tokenId = RefreshTokenId.newId();
        UserId userId = UserId.newId();
        RefreshToken token = issue(tokenId, userId, NOW.plusSeconds(3600));

        assertThat(token.refreshTokenId()).isEqualTo(tokenId);
        assertThat(token.userId()).isEqualTo(userId);
        assertThat(token.tenantId()).isEqualTo(TENANT_ID);
        assertThat(token.sessionId()).isEqualTo(SESSION_ID);
        assertThat(token.tokenHash()).isEqualTo(HASH);
        assertThat(token.issuedAt()).isEqualTo(NOW);
        assertThat(token.expiresAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(token.status()).isEqualTo(TokenStatus.ACTIVE);
        assertThat(token.replacedByTokenId()).isNull();
        assertThat(token.isUsableAt(NOW.plusSeconds(1))).isTrue();
        assertThat(token.isUsableAt(NOW.plusSeconds(3600))).isFalse();
        RefreshTokenCreated created = (RefreshTokenCreated) token.domainEvents().getFirst();
        assertThat(created.aggregateId()).isEqualTo(tokenId.toAggregateId());

        token.revoke(ACTOR_ID, NOW.plusSeconds(10));

        assertThat(token.status()).isEqualTo(TokenStatus.REVOKED);
        assertThat(token.isUsableAt(NOW.plusSeconds(11))).isFalse();
        assertThat(token.domainEvents().getLast()).isInstanceOf(RefreshTokenRevoked.class);
        assertThatThrownBy(() -> token.revoke(ACTOR_ID, NOW.plusSeconds(20)))
                .isInstanceOf(IdentityDomainException.class);
    }

    @Test
    void expiresOnlyAtOrAfterExpiry() {
        RefreshToken token = issue(RefreshTokenId.newId(), UserId.newId(), NOW.plusSeconds(60));

        assertThat(token.expire(ACTOR_ID, NOW.plusSeconds(59))).isFalse();
        assertThat(token.expire(ACTOR_ID, NOW.plusSeconds(60))).isTrue();
        assertThat(token.status()).isEqualTo(TokenStatus.EXPIRED);
        assertThat(token.expire(ACTOR_ID, NOW.plusSeconds(61))).isFalse();
        assertThatThrownBy(() -> token.revoke(ACTOR_ID, NOW.plusSeconds(61)))
                .isInstanceOf(IdentityDomainException.class);
    }

    @Test
    void rotatesAndDetectsReuse() {
        RefreshToken token = issue(RefreshTokenId.newId(), UserId.newId(), NOW.plusSeconds(60));
        RefreshTokenId replacementId = RefreshTokenId.newId();

        token.rotate(replacementId, ACTOR_ID, NOW.plusSeconds(10));

        assertThat(token.status()).isEqualTo(TokenStatus.ROTATED);
        assertThat(token.replacedByTokenId()).isEqualTo(replacementId);
        assertThatThrownBy(() -> token.rotate(RefreshTokenId.newId(), ACTOR_ID, NOW.plusSeconds(11)))
                .isInstanceOf(IdentityDomainException.class);

        token.markReused(ACTOR_ID, NOW.plusSeconds(12));

        assertThat(token.status()).isEqualTo(TokenStatus.REUSED);
        assertThatThrownBy(() -> token.markReused(ACTOR_ID, NOW.plusSeconds(13)))
                .isInstanceOf(IdentityDomainException.class);
    }

    @Test
    void rejectsSelfRotationExpiredRotationAndInvalidRehydration() {
        RefreshToken token = issue(RefreshTokenId.newId(), UserId.newId(), NOW.plusSeconds(60));

        assertThatThrownBy(() -> token.rotate(token.refreshTokenId(), ACTOR_ID, NOW.plusSeconds(1)))
                .isInstanceOf(IdentityDomainException.class);
        assertThatThrownBy(() -> token.rotate(RefreshTokenId.newId(), ACTOR_ID, NOW.plusSeconds(60)))
                .isInstanceOf(IdentityDomainException.class);
        assertThatIllegalArgumentException().isThrownBy(() -> RefreshToken.rehydrate(
                token.refreshTokenId(),
                token.userId(),
                TENANT_ID,
                SESSION_ID,
                HASH,
                NOW,
                NOW.plusSeconds(60),
                TokenStatus.ROTATED,
                null,
                AuditInfo.createdBy(ACTOR_ID, NOW),
                1));
    }

    @Test
    void rejectsInvalidExpiryAndUsesTokenIdentityForEquality() {
        RefreshTokenId tokenId = RefreshTokenId.newId();
        UserId userId = UserId.newId();
        RefreshToken first = issue(tokenId, userId, NOW.plusSeconds(60));
        RefreshToken sameIdentity = issue(tokenId, userId, NOW.plusSeconds(120));
        RefreshToken different = issue(RefreshTokenId.newId(), userId, NOW.plusSeconds(60));

        assertThat(first).isEqualTo(first).isEqualTo(sameIdentity).hasSameHashCodeAs(sameIdentity);
        assertThat(first).isNotEqualTo(different).isNotEqualTo(null).isNotEqualTo("token");
        assertThatIllegalArgumentException().isThrownBy(() -> RefreshToken.issue(
                RefreshTokenId.newId(), userId, TENANT_ID, SESSION_ID, HASH, NOW, NOW, ACTOR_ID));
    }

    @Test
    void rehydratesTerminalStateWithoutEvents() {
        RefreshTokenId replacementId = RefreshTokenId.newId();
        AuditInfo auditInfo = AuditInfo.createdBy(ACTOR_ID, NOW);
        RefreshToken token = RefreshToken.rehydrate(
                RefreshTokenId.newId(),
                UserId.newId(),
                TENANT_ID,
                SESSION_ID,
                HASH,
                NOW,
                NOW.plusSeconds(60),
                TokenStatus.REUSED,
                replacementId,
                auditInfo,
                5);

        assertThat(token.status()).isEqualTo(TokenStatus.REUSED);
        assertThat(token.replacedByTokenId()).isEqualTo(replacementId);
        assertThat(token.auditInfo()).isEqualTo(auditInfo);
        assertThat(token.version()).isEqualTo(5);
        assertThat(token.domainEvents()).isEmpty();
    }

    private RefreshToken issue(RefreshTokenId tokenId, UserId userId, Instant expiresAt) {
        return RefreshToken.issue(
                tokenId,
                userId,
                TENANT_ID,
                SESSION_ID,
                HASH,
                NOW,
                expiresAt,
                ACTOR_ID);
    }
}
