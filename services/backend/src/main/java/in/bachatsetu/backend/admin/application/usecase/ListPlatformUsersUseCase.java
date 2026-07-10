package in.bachatsetu.backend.admin.application.usecase;

import in.bachatsetu.backend.admin.application.query.PlatformUserResult;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.domain.port.PlatformUserSearchCriteria;

/** Searches platform users, across every tenant, by any combination of status/email/phone/date range. */
@FunctionalInterface
public interface ListPlatformUsersUseCase {

    PlatformPage<PlatformUserResult> execute(PlatformUserSearchCriteria criteria);
}
