package in.bachatsetu.backend.admin.application.mapper;

import in.bachatsetu.backend.admin.application.query.PlatformGroupResult;
import in.bachatsetu.backend.admin.application.query.PlatformStatisticsResult;
import in.bachatsetu.backend.admin.application.query.PlatformTenantResult;
import in.bachatsetu.backend.admin.application.query.PlatformUserResult;
import in.bachatsetu.backend.admin.domain.model.PlatformGroupSummary;
import in.bachatsetu.backend.admin.domain.model.PlatformStatistics;
import in.bachatsetu.backend.admin.domain.model.PlatformTenantSummary;
import in.bachatsetu.backend.admin.domain.model.PlatformUserSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import java.util.Objects;

/** Converts Admin domain summaries to application-layer read models. */
public final class AdminApplicationMapper {

    public PlatformUserResult toResult(PlatformUserSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        return new PlatformUserResult(
                summary.userId().value(),
                summary.tenantId().value(),
                summary.email(),
                summary.phoneNumber(),
                summary.firstName(),
                summary.lastName(),
                summary.status(),
                summary.createdAt());
    }

    public PlatformPage<PlatformUserResult> toResultPage(PlatformPage<PlatformUserSummary> page) {
        Objects.requireNonNull(page, "page must not be null");
        return new PlatformPage<>(
                page.content().stream().map(this::toResult).toList(), page.page(), page.size(),
                page.totalElements());
    }

    public PlatformGroupResult toResult(PlatformGroupSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        return new PlatformGroupResult(
                summary.groupId().value(),
                summary.tenantId().value(),
                summary.code(),
                summary.name(),
                summary.status(),
                summary.memberCount(),
                summary.createdAt());
    }

    public PlatformPage<PlatformGroupResult> toGroupResultPage(PlatformPage<PlatformGroupSummary> page) {
        Objects.requireNonNull(page, "page must not be null");
        return new PlatformPage<>(
                page.content().stream().map(this::toResult).toList(), page.page(), page.size(),
                page.totalElements());
    }

    public PlatformTenantResult toResult(PlatformTenantSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        return new PlatformTenantResult(summary.tenantId().value(), summary.userCount(), summary.groupCount());
    }

    public PlatformPage<PlatformTenantResult> toTenantResultPage(PlatformPage<PlatformTenantSummary> page) {
        Objects.requireNonNull(page, "page must not be null");
        return new PlatformPage<>(
                page.content().stream().map(this::toResult).toList(), page.page(), page.size(),
                page.totalElements());
    }

    public PlatformStatisticsResult toResult(PlatformStatistics statistics) {
        Objects.requireNonNull(statistics, "statistics must not be null");
        return new PlatformStatisticsResult(
                statistics.totalUsers(),
                statistics.activeUsers(),
                statistics.disabledUsers(),
                statistics.totalGroups(),
                statistics.activeGroups(),
                statistics.totalPayments(),
                statistics.completedPayments(),
                statistics.totalReceipts(),
                statistics.totalNotifications(),
                statistics.totalFiles());
    }
}
