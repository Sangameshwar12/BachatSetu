package in.bachatsetu.backend.admin.application.usecase;

import in.bachatsetu.backend.admin.application.query.PlatformGroupResult;
import in.bachatsetu.backend.admin.domain.port.PlatformGroupSearchCriteria;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;

/** Searches savings groups, across every tenant, by status and date range. */
@FunctionalInterface
public interface ListPlatformGroupsUseCase {

    PlatformPage<PlatformGroupResult> execute(PlatformGroupSearchCriteria criteria);
}
