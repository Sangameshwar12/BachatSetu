package in.bachatsetu.backend.auth.application.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import in.bachatsetu.backend.auth.application.token.command.GenerateAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.command.GenerateRefreshTokenCommand;
import in.bachatsetu.backend.auth.application.token.command.RefreshAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.command.RevokeRefreshTokenCommand;
import in.bachatsetu.backend.auth.application.token.command.ValidateAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.exception.TokenApplicationException;
import in.bachatsetu.backend.auth.application.token.exception.TokenFailureReason;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenClaims;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenPrincipal;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenValue;
import in.bachatsetu.backend.auth.application.token.port.IssuedAccessToken;
import in.bachatsetu.backend.auth.application.token.port.IssuedRefreshToken;
import in.bachatsetu.backend.auth.application.token.port.JwtValidationException;
import in.bachatsetu.backend.auth.application.token.port.JwtValidationFailure;
import in.bachatsetu.backend.auth.application.token.port.RefreshTokenCredential;
import in.bachatsetu.backend.auth.application.token.query.RefreshTokenResult;
import in.bachatsetu.backend.auth.application.token.query.RefreshTokenState;
import in.bachatsetu.backend.auth.application.token.query.TokenPairResult;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenHash;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.TokenSessionId;
import in.bachatsetu.backend.auth.domain.model.TokenStatus;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TokenContractTest {

    private static final Instant NOW = Instant.parse("2026-07-05T06:00:00Z");

    @Test
    void createsRedactingTokenPortValuesAndResults() {
        UserId userId = UserId.newId();
        AggregateId tenantId = AggregateId.newId();
        AccessTokenPrincipal principal = new AccessTokenPrincipal(
                userId,
                MobileNumber.of("+919876543210"),
                tenantId,
                Set.of("MEMBER"),
                Set.of("group:read"));
        AccessTokenClaims claims = new AccessTokenClaims(
                userId,
                principal.mobileNumber(),
                tenantId,
                principal.roles(),
                principal.permissions(),
                NOW,
                NOW.plusSeconds(900),
                "bachatsetu",
                "bachatsetu-api",
                1);
        AccessTokenValue accessValue = AccessTokenValue.of("header.payload.signature");
        IssuedAccessToken access = new IssuedAccessToken(accessValue, claims);
        RefreshTokenId refreshId = RefreshTokenId.newId();
        RefreshTokenCredential credential = RefreshTokenCredential.create(refreshId, "S".repeat(43));
        IssuedRefreshToken issuedRefresh = new IssuedRefreshToken(
                credential, RefreshTokenHash.encoded("H".repeat(60)));
        TokenSessionId sessionId = TokenSessionId.newId();
        RefreshTokenResult refresh = new RefreshTokenResult(credential, sessionId, NOW.plusSeconds(3600));
        TokenPairResult pair = new TokenPairResult(access, refresh);
        RefreshTokenState state = new RefreshTokenState(refreshId, TokenStatus.REVOKED);

        assertThat(principal.roles()).containsExactly("MEMBER");
        assertThat(claims.permissions()).containsExactly("group:read");
        assertThat(access.expiresAt()).isEqualTo(NOW.plusSeconds(900));
        assertThat(accessValue.toString()).isEqualTo("[REDACTED]");
        assertThat(credential.value()).startsWith(refreshId.toString());
        assertThat(credential.secret()).isEqualTo("S".repeat(43));
        assertThat(credential.toString()).isEqualTo("[REDACTED]");
        assertThat(RefreshTokenCredential.parse(credential.value()).tokenId()).isEqualTo(refreshId);
        assertThat(issuedRefresh.hash().toString()).isEqualTo("[REDACTED]");
        assertThat(pair.refreshToken()).isEqualTo(refresh);
        assertThat(state.status()).isEqualTo(TokenStatus.REVOKED);
    }

    @Test
    void validatesSensitiveValueFormats() {
        assertThatIllegalArgumentException().isThrownBy(() -> AccessTokenValue.of(" "));
        assertThatIllegalArgumentException().isThrownBy(() -> RefreshTokenCredential.parse("bad"));
        assertThatIllegalArgumentException().isThrownBy(() -> RefreshTokenCredential.parse("a.b.c"));
        assertThatIllegalArgumentException().isThrownBy(() -> RefreshTokenCredential.create(
                RefreshTokenId.newId(), "short"));
        assertThatIllegalArgumentException().isThrownBy(() -> new AccessTokenClaims(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                Set.of(),
                Set.of(),
                NOW,
                NOW,
                "issuer",
                "audience",
                0));
        assertThatIllegalArgumentException().isThrownBy(() -> RefreshTokenHash.encoded("short"));
        assertThatIllegalArgumentException().isThrownBy(() -> RefreshTokenHash.encoded("H".repeat(256)));
        assertThatNullPointerException().isThrownBy(() -> AccessTokenValue.of(null));
    }

    @Test
    void createsCommandsAndRedactsCredentialCommands() {
        UserId userId = UserId.newId();
        AggregateId tenantId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        TokenSessionId sessionId = TokenSessionId.newId();
        String rawRefresh = RefreshTokenId.newId() + "." + "S".repeat(43);
        GenerateAccessTokenCommand access = new GenerateAccessTokenCommand(userId, tenantId);
        GenerateRefreshTokenCommand refresh = new GenerateRefreshTokenCommand(
                userId, tenantId, sessionId, actorId);
        RefreshAccessTokenCommand rotate = new RefreshAccessTokenCommand(rawRefresh, actorId);
        RevokeRefreshTokenCommand revoke = new RevokeRefreshTokenCommand(rawRefresh, actorId);
        ValidateAccessTokenCommand validate = new ValidateAccessTokenCommand(AccessTokenValue.of("a.b.c"));

        assertThat(access.userId()).isEqualTo(userId);
        assertThat(refresh.sessionId()).isEqualTo(sessionId);
        assertThat(rotate.refreshToken()).isEqualTo(rawRefresh);
        assertThat(rotate.actorId()).isEqualTo(actorId);
        assertThat(rotate.toString()).doesNotContain(rawRefresh);
        assertThat(revoke.refreshToken()).isEqualTo(rawRefresh);
        assertThat(revoke.actorId()).isEqualTo(actorId);
        assertThat(revoke.toString()).doesNotContain(rawRefresh);
        assertThat(validate.token().value()).isEqualTo("a.b.c");
        assertThatIllegalArgumentException().isThrownBy(() -> new RefreshAccessTokenCommand(" ", actorId));
        assertThatIllegalArgumentException().isThrownBy(() -> new RevokeRefreshTokenCommand(" ", actorId));
    }

    @Test
    void exposesStableFailureReasons() {
        TokenApplicationException application = new TokenApplicationException(
                TokenFailureReason.REFRESH_TOKEN_REUSED, "reuse");
        JwtValidationException validation = new JwtValidationException(
                JwtValidationFailure.INVALID_SIGNATURE, "signature");

        assertThat(application.reason()).isEqualTo(TokenFailureReason.REFRESH_TOKEN_REUSED);
        assertThat(validation.failure()).isEqualTo(JwtValidationFailure.INVALID_SIGNATURE);
        assertThat(application).hasMessage("reuse");
        assertThat(validation).hasMessage("signature");
    }

    @Test
    void createsAndParsesSessionIdentifiers() {
        TokenSessionId generated = TokenSessionId.newId();

        assertThat(TokenSessionId.from(generated.toString())).isEqualTo(generated);
        assertThat(generated.toString()).isEqualTo(generated.value().toString());
    }
}
