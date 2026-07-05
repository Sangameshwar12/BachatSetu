package in.bachatsetu.backend.auth.application.token.port;

import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Validated access-token claims returned by the JWT provider boundary. */
public record AccessTokenClaims(
        UserId userId,
        MobileNumber mobileNumber,
        AggregateId tenantId,
        Set<String> roles,
        Set<String> permissions,
        Instant issuedAt,
        Instant expiresAt,
        String issuer,
        String audience,
        int version) {

    public AccessTokenClaims {
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(mobileNumber, "mobile number must not be null");
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        roles = Set.copyOf(new TreeSet<>(Objects.requireNonNull(roles, "roles must not be null")));
        permissions = Set.copyOf(new TreeSet<>(Objects.requireNonNull(permissions, "permissions must not be null")));
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(issuer, "issuer must not be null");
        Objects.requireNonNull(audience, "audience must not be null");
        if (!expiresAt.isAfter(issuedAt) || version < 1) {
            throw new IllegalArgumentException("access token temporal or version claims are invalid");
        }
    }
}
