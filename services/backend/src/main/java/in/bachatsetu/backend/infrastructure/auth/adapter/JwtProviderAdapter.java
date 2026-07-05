package in.bachatsetu.backend.infrastructure.auth.adapter;

import in.bachatsetu.backend.auth.application.token.port.AccessTokenClaims;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenPrincipal;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenValue;
import in.bachatsetu.backend.auth.application.token.port.IssuedAccessToken;
import in.bachatsetu.backend.auth.application.token.port.JwtProviderPort;
import in.bachatsetu.backend.auth.application.token.port.JwtValidationException;
import in.bachatsetu.backend.auth.application.token.port.JwtValidationFailure;
import in.bachatsetu.backend.auth.application.token.port.TokenClockPort;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.infrastructure.auth.config.AuthenticationTokenProperties;
import in.bachatsetu.backend.shared.domain.AggregateId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import javax.crypto.SecretKey;

/** JJWT HS512 adapter; all application contracts remain algorithm independent. */
public final class JwtProviderAdapter implements JwtProviderPort {

    private static final String MOBILE_CLAIM = "mobile_number";
    private static final String TENANT_CLAIM = "tenant_id";
    private static final String ROLES_CLAIM = "roles";
    private static final String PERMISSIONS_CLAIM = "permissions";
    private static final String VERSION_CLAIM = "ver";

    private final AuthenticationTokenProperties properties;
    private final TokenClockPort clock;
    private final SecretKey signingKey;

    public JwtProviderAdapter(
            AuthenticationTokenProperties properties,
            TokenClockPort clock) {
        this.properties = Objects.requireNonNull(properties, "token properties must not be null");
        this.clock = Objects.requireNonNull(clock, "token clock must not be null");
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.signingSecret()));
    }

    @Override
    public IssuedAccessToken issue(AccessTokenPrincipal principal) {
        Objects.requireNonNull(principal, "access token principal must not be null");
        Instant issuedAt = clock.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenExpiry());
        String compact = Jwts.builder()
                .issuer(properties.issuer())
                .subject(principal.userId().toString())
                .audience().add(properties.audience()).and()
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .id(UUID.randomUUID().toString())
                .claim(MOBILE_CLAIM, principal.mobileNumber().value())
                .claim(TENANT_CLAIM, principal.tenantId().toString())
                .claim(ROLES_CLAIM, principal.roles().stream().sorted().toList())
                .claim(PERMISSIONS_CLAIM, principal.permissions().stream().sorted().toList())
                .claim(VERSION_CLAIM, properties.jwtVersion())
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
        AccessTokenClaims claims = new AccessTokenClaims(
                principal.userId(),
                principal.mobileNumber(),
                principal.tenantId(),
                principal.roles(),
                principal.permissions(),
                issuedAt,
                expiresAt,
                properties.issuer(),
                properties.audience(),
                properties.jwtVersion());
        return new IssuedAccessToken(AccessTokenValue.of(compact), claims);
    }

    @Override
    public AccessTokenClaims validate(AccessTokenValue token) {
        Objects.requireNonNull(token, "access token must not be null");
        try {
            Jws<Claims> parsed = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(clock.now()))
                    .clockSkewSeconds(properties.clockSkew().toSeconds())
                    .requireIssuer(properties.issuer())
                    .requireAudience(properties.audience())
                    .sig().clear().add(Jwts.SIG.HS512).and()
                    .build()
                    .parseSignedClaims(token.value());
            return toClaims(parsed.getPayload());
        } catch (ExpiredJwtException exception) {
            throw failure(JwtValidationFailure.EXPIRED, "access token has expired");
        } catch (SignatureException | SecurityException exception) {
            throw failure(JwtValidationFailure.INVALID_SIGNATURE, "access token signature is invalid");
        } catch (MalformedJwtException exception) {
            throw failure(JwtValidationFailure.MALFORMED, "access token is malformed");
        } catch (UnsupportedJwtException exception) {
            throw failure(JwtValidationFailure.UNSUPPORTED, "access token algorithm or format is unsupported");
        } catch (JwtException | IllegalArgumentException exception) {
            throw failure(JwtValidationFailure.INVALID_CLAIMS, "access token claims are invalid");
        }
    }

    private AccessTokenClaims toClaims(Claims claims) {
        int version = claims.get(VERSION_CLAIM, Integer.class);
        if (version != properties.jwtVersion()) {
            throw new IllegalArgumentException("JWT version is not accepted");
        }
        return new AccessTokenClaims(
                UserId.from(claims.getSubject()),
                MobileNumber.of(claims.get(MOBILE_CLAIM, String.class)),
                AggregateId.from(claims.get(TENANT_CLAIM, String.class)),
                stringSet(claims.get(ROLES_CLAIM), ROLES_CLAIM),
                stringSet(claims.get(PERMISSIONS_CLAIM), PERMISSIONS_CLAIM),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant(),
                claims.getIssuer(),
                properties.audience(),
                version);
    }

    private Set<String> stringSet(Object claim, String name) {
        if (!(claim instanceof Collection<?> values)
                || values.stream().anyMatch(value -> !(value instanceof String))) {
            throw new IllegalArgumentException(name + " claim is invalid");
        }
        Set<String> result = new TreeSet<>();
        values.forEach(value -> result.add((String) value));
        return Set.copyOf(result);
    }

    private JwtValidationException failure(JwtValidationFailure failure, String message) {
        return new JwtValidationException(failure, message);
    }
}
