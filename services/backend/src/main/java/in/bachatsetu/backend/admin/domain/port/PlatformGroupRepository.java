package in.bachatsetu.backend.admin.domain.port;

import in.bachatsetu.backend.admin.domain.model.PlatformGroupSummary;

/** Cross-tenant read access to savings groups, for platform administration listing only. */
public interface PlatformGroupRepository {

    PlatformPage<PlatformGroupSummary> search(PlatformGroupSearchCriteria criteria);
}
