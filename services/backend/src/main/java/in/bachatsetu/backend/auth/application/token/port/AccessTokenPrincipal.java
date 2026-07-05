package in.bachatsetu.backend.auth.application.token.port;

import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Trusted identity and authorization snapshot used to issue an access token. */
public record AccessTokenPrincipal(
        UserId userId,
        MobileNumber mobileNumber,
        AggregateId tenantId,
        Set<String> roles,
        Set<String> permissions) {

    public AccessTokenPrincipal {
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(mobileNumber, "mobile number must not be null");
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        roles = Set.copyOf(new TreeSet<>(Objects.requireNonNull(roles, "roles must not be null")));
        permissions = Set.copyOf(new TreeSet<>(Objects.requireNonNull(permissions, "permissions must not be null")));
    }
}
