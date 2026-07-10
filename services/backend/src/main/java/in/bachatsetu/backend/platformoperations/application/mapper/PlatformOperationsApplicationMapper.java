package in.bachatsetu.backend.platformoperations.application.mapper;

import in.bachatsetu.backend.platformoperations.application.query.AnnouncementResult;
import in.bachatsetu.backend.platformoperations.application.query.TenantResult;
import in.bachatsetu.backend.platformoperations.domain.model.Announcement;
import in.bachatsetu.backend.platformoperations.domain.model.Tenant;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatistics;
import java.time.Instant;

public final class PlatformOperationsApplicationMapper {

    public TenantResult toResult(Tenant tenant, TenantStatistics statistics) {
        return new TenantResult(tenant.id(), tenant.status(), tenant.suspensionReason(), statistics);
    }

    public AnnouncementResult toResult(Announcement announcement, Instant now) {
        return new AnnouncementResult(
                announcement.id(), announcement.title(), announcement.message(), announcement.startAt(),
                announcement.endAt(), announcement.severity(), announcement.isActive(now));
    }
}
