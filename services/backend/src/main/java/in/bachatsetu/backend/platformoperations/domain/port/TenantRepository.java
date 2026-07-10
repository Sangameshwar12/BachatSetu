package in.bachatsetu.backend.platformoperations.domain.port;

import in.bachatsetu.backend.platformoperations.domain.model.Tenant;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;

/** Persistence boundary for the platform-wide {@link Tenant} lifecycle record. */
public interface TenantRepository {

    void save(Tenant tenant);

    Optional<Tenant> findById(AggregateId tenantId);
}
