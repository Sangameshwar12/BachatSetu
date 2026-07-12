package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.auth.domain.port.TenantProvider;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Resolves the tenant for a brand-new self-registration by delegating to the same {@link
 * TenantScopeProvider} the pre-login OTP-request and login-completion adapters already use — see
 * {@link TenantScopeProviderConfig} for the full rationale.
 */
@Component
@ConditionalOnPersistenceRepositories
public class SignupTenantResolverAdapter implements TenantProvider {

    private final TenantScopeProvider tenantScopeProvider;

    public SignupTenantResolverAdapter(TenantScopeProvider tenantScopeProvider) {
        this.tenantScopeProvider = Objects.requireNonNull(tenantScopeProvider, "tenantScopeProvider must not be null");
    }

    @Override
    public AggregateId currentTenantId() {
        return tenantScopeProvider.currentTenantId();
    }
}
