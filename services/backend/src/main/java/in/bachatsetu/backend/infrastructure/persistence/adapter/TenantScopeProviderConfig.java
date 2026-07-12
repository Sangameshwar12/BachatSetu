package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Resolves the single tenant this deployment serves.
 *
 * <p>Authentication's pre-login repository adapters ({@link AuthUserRepositoryAdapter},
 * {@link RoleRepositoryAdapter}, {@link UserRepositoryAdapter}) are tenant-scoped, but no request
 * carries a tenant identifier during login/OTP flows, and no real multi-tenant resolution
 * strategy (header, subdomain, or similar) has been designed yet — {@code platform.tenants}
 * tracks tenant lifecycle (active/suspended/archived) but nothing yet resolves *which* tenant an
 * unauthenticated request belongs to from the request itself. There is currently exactly one
 * tenant per deployment, so returning one configured tenant id is correct, not a compromise —
 * see {@link in.bachatsetu.backend.auth.application.security.AuthenticatedUser#tenantId()} for
 * how already-authenticated requests use the real per-session tenant instead (resolved from the
 * JWT, itself populated from this same bean at login time).
 *
 * <p>The tenant id is supplied through {@code bachatsetu.tenancy.default-tenant-id}
 * ({@code TENANT_DEFAULT_ID}) rather than hardcoded, so it is identical to whatever an operator
 * provisions in {@code platform.tenants}, and is available in every profile — {@code local}'s
 * value continues to match the id the local seed loader writes into every locally seeded record.
 */
@Configuration(proxyBeanMethods = false)
public class TenantScopeProviderConfig {

    @Bean
    TenantScopeProvider tenantScopeProvider(
            @Value("${bachatsetu.tenancy.default-tenant-id}") UUID defaultTenantId) {
        AggregateId tenantId = new AggregateId(defaultTenantId);
        return () -> tenantId;
    }
}
