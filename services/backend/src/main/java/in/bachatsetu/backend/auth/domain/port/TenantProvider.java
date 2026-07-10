package in.bachatsetu.backend.auth.domain.port;

import in.bachatsetu.backend.shared.domain.AggregateId;

/**
 * Resolves the tenant a brand-new self-registration belongs to. Signup happens before any JWT
 * exists, so the caller's tenant cannot come from an {@code AuthenticatedUser} — this mirrors the
 * same pre-login tenant resolution seam the existing OTP-request flow already relies on.
 */
@FunctionalInterface
public interface TenantProvider {

    AggregateId currentTenantId();
}
