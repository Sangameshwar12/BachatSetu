package in.bachatsetu.backend.infrastructure.auth.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.application.token.port.AccessTokenClaims;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenPrincipal;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenValue;
import in.bachatsetu.backend.auth.application.token.port.JwtValidationException;
import in.bachatsetu.backend.auth.application.token.port.JwtValidationFailure;
import in.bachatsetu.backend.auth.application.token.port.TokenClockPort;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.infrastructure.auth.config.AuthenticationTokenProperties;
import in.bachatsetu.backend.shared.domain.AggregateId;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtProviderAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-05T06:00:00Z");
    private static final String SECRET = Base64.getEncoder().encodeToString("K".repeat(64).getBytes());

    private final AtomicReference<Instant> currentTime = new AtomicReference<>(NOW);
    private final TokenClockPort clock = currentTime::get;
    private AuthenticationTokenProperties properties;
    private JwtProviderAdapter adapter;
    private AccessTokenPrincipal principal;

    @BeforeEach
    void setUp() {
        properties = properties(SECRET, Duration.ofSeconds(30), 1);
        adapter = new JwtProviderAdapter(properties, clock);
        principal = new AccessTokenPrincipal(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                Set.of("GROUP_MEMBER", "ORGANIZER"),
                Set.of("group.read", "group.write"));
    }

    @Test
    void generatesAndValidatesHs512TokenClaims() {
        var issued = adapter.issue(principal);

        AccessTokenClaims claims = adapter.validate(issued.token());

        assertThat(issued.token().value().split("\\.")).hasSize(3);
        assertThat(claims.userId()).isEqualTo(principal.userId());
        assertThat(claims.mobileNumber()).isEqualTo(principal.mobileNumber());
        assertThat(claims.tenantId()).isEqualTo(principal.tenantId());
        assertThat(claims.roles()).containsExactlyInAnyOrderElementsOf(principal.roles());
        assertThat(claims.permissions()).containsExactlyInAnyOrderElementsOf(principal.permissions());
        assertThat(claims.issuedAt()).isEqualTo(NOW);
        assertThat(claims.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(claims.issuer()).isEqualTo("bachatsetu");
        assertThat(claims.audience()).isEqualTo("bachatsetu-api");
        assertThat(claims.version()).isOne();
    }

    @Test
    void acceptsConfiguredClockSkewThenRejectsExpiration() {
        var issued = adapter.issue(principal);
        currentTime.set(issued.expiresAt().plusSeconds(29));

        assertThat(adapter.validate(issued.token()).userId()).isEqualTo(principal.userId());

        currentTime.set(issued.expiresAt().plusSeconds(31));
        assertFailure(() -> adapter.validate(issued.token()), JwtValidationFailure.EXPIRED);
    }

    @Test
    void rejectsTamperedMalformedAndUnsupportedTokens() {
        String valid = adapter.issue(principal).token().value();
        int signatureStart = valid.lastIndexOf('.') + 1;
        char signatureCharacter = valid.charAt(signatureStart);
        String tampered = valid.substring(0, signatureStart)
                + (signatureCharacter == 'A' ? 'B' : 'A')
                + valid.substring(signatureStart + 1);
        assertFailure(() -> adapter.validate(AccessTokenValue.of(tampered)), JwtValidationFailure.INVALID_SIGNATURE);
        assertFailure(
                () -> adapter.validate(AccessTokenValue.of("not.a.jwt")),
                JwtValidationFailure.MALFORMED);

        String hs256 = baseBuilder()
                .claim("mobile_number", principal.mobileNumber().value())
                .claim("tenant_id", principal.tenantId().toString())
                .claim("roles", principal.roles())
                .claim("permissions", principal.permissions())
                .claim("ver", 1)
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
        assertFailure(() -> adapter.validate(AccessTokenValue.of(hs256)), JwtValidationFailure.INVALID_SIGNATURE);

        String unsecured = baseBuilder()
                .claim("mobile_number", principal.mobileNumber().value())
                .claim("tenant_id", principal.tenantId().toString())
                .claim("roles", principal.roles())
                .claim("permissions", principal.permissions())
                .claim("ver", 1)
                .compact();
        assertFailure(() -> adapter.validate(AccessTokenValue.of(unsecured)), JwtValidationFailure.UNSUPPORTED);
    }

    @Test
    void rejectsMissingInvalidAndOutdatedClaims() {
        String missingRoles = baseBuilder()
                .claim("mobile_number", principal.mobileNumber().value())
                .claim("tenant_id", principal.tenantId().toString())
                .claim("permissions", principal.permissions())
                .claim("ver", 1)
                .signWith(key(), Jwts.SIG.HS512)
                .compact();
        assertFailure(
                () -> adapter.validate(AccessTokenValue.of(missingRoles)),
                JwtValidationFailure.INVALID_CLAIMS);

        String invalidRoles = baseBuilder()
                .claim("mobile_number", principal.mobileNumber().value())
                .claim("tenant_id", principal.tenantId().toString())
                .claim("roles", "GROUP_MEMBER")
                .claim("permissions", principal.permissions())
                .claim("ver", 1)
                .signWith(key(), Jwts.SIG.HS512)
                .compact();
        assertFailure(
                () -> adapter.validate(AccessTokenValue.of(invalidRoles)),
                JwtValidationFailure.INVALID_CLAIMS);

        String oldVersion = baseBuilder()
                .claim("mobile_number", principal.mobileNumber().value())
                .claim("tenant_id", principal.tenantId().toString())
                .claim("roles", principal.roles())
                .claim("permissions", principal.permissions())
                .claim("ver", 2)
                .signWith(key(), Jwts.SIG.HS512)
                .compact();
        assertFailure(
                () -> adapter.validate(AccessTokenValue.of(oldVersion)),
                JwtValidationFailure.INVALID_CLAIMS);
    }

    @Test
    void rejectsInvalidIssuerAsInvalidClaims() {
        String wrongIssuer = Jwts.builder()
                .issuer("someone-else")
                .subject(principal.userId().toString())
                .audience().add("bachatsetu-api").and()
                .issuedAt(Date.from(NOW))
                .expiration(Date.from(NOW.plusSeconds(900)))
                .claim("mobile_number", principal.mobileNumber().value())
                .claim("tenant_id", principal.tenantId().toString())
                .claim("roles", principal.roles())
                .claim("permissions", principal.permissions())
                .claim("ver", 1)
                .signWith(key(), Jwts.SIG.HS512)
                .compact();

        assertFailure(
                () -> adapter.validate(AccessTokenValue.of(wrongIssuer)),
                JwtValidationFailure.INVALID_CLAIMS);
    }

    private io.jsonwebtoken.JwtBuilder baseBuilder() {
        return Jwts.builder()
                .issuer("bachatsetu")
                .subject(principal.userId().toString())
                .audience().add("bachatsetu-api").and()
                .issuedAt(Date.from(NOW))
                .expiration(Date.from(NOW.plusSeconds(900)));
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    private AuthenticationTokenProperties properties(String secret, Duration skew, int version) {
        return new AuthenticationTokenProperties(
                true,
                Duration.ofMinutes(15),
                Duration.ofDays(30),
                "bachatsetu",
                "bachatsetu-api",
                secret,
                skew,
                12,
                version);
    }

    private void assertFailure(Runnable action, JwtValidationFailure failure) {
        assertThatThrownBy(action::run)
                .isInstanceOf(JwtValidationException.class)
                .extracting(exception -> ((JwtValidationException) exception).failure())
                .isEqualTo(failure);
    }
}
