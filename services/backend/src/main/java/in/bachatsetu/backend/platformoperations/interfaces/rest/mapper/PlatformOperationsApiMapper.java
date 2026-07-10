package in.bachatsetu.backend.platformoperations.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.platformoperations.application.command.ActivateTenantCommand;
import in.bachatsetu.backend.platformoperations.application.command.ArchiveTenantCommand;
import in.bachatsetu.backend.platformoperations.application.command.PublishAnnouncementCommand;
import in.bachatsetu.backend.platformoperations.application.command.SendBroadcastNotificationCommand;
import in.bachatsetu.backend.platformoperations.application.command.SuspendTenantCommand;
import in.bachatsetu.backend.platformoperations.application.query.AnnouncementResult;
import in.bachatsetu.backend.platformoperations.application.query.BroadcastResult;
import in.bachatsetu.backend.platformoperations.application.query.PlatformOverviewResult;
import in.bachatsetu.backend.platformoperations.application.query.SystemHealthResult;
import in.bachatsetu.backend.platformoperations.application.query.TenantResult;
import in.bachatsetu.backend.platformoperations.application.usecase.GetTenantUseCase;
import in.bachatsetu.backend.platformoperations.domain.model.AnnouncementSeverity;
import in.bachatsetu.backend.platformoperations.domain.model.BroadcastScope;
import in.bachatsetu.backend.platformoperations.domain.model.ComponentHealth;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatus;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.AnnouncementResponse;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.BroadcastRequest;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.BroadcastResponse;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.PlatformOperationsPageResponse;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.PlatformOverviewResponse;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.PublishAnnouncementRequest;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.SuspendTenantRequest;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.SystemHealthResponse;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.TenantResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.shared.domain.PageQuery;
import org.springframework.stereotype.Component;

@Component
public class PlatformOperationsApiMapper {

    public SuspendTenantCommand toSuspendCommand(String tenantId, AuthenticatedUser currentUser, SuspendTenantRequest request) {
        return new SuspendTenantCommand(
                AggregateId.from(tenantId), request.reason(), currentUser.userId().toAggregateId());
    }

    public ActivateTenantCommand toActivateCommand(String tenantId, AuthenticatedUser currentUser) {
        return new ActivateTenantCommand(AggregateId.from(tenantId), currentUser.userId().toAggregateId());
    }

    public ArchiveTenantCommand toArchiveCommand(String tenantId, AuthenticatedUser currentUser) {
        return new ArchiveTenantCommand(AggregateId.from(tenantId), currentUser.userId().toAggregateId());
    }

    public TenantResponse get(GetTenantUseCase useCase, String tenantId) {
        return toResponse(useCase.execute(AggregateId.from(tenantId)));
    }

    public TenantResponse toResponse(TenantResult result) {
        return new TenantResponse(
                result.tenantId().toString(), result.status().name(), result.suspensionReason(),
                result.statistics().users(), result.statistics().groups(), result.statistics().payments(),
                result.statistics().revenuePaise(), result.statistics().storageFiles(),
                result.statistics().storageBytes(), result.statistics().notifications(),
                result.statistics().lastActivityAt());
    }

    public PlatformOperationsPageResponse<TenantResponse> toTenantPageResponse(Page<TenantResult> page) {
        return new PlatformOperationsPageResponse<>(
                page.content().stream().map(this::toResponse).toList(), page.page(), page.size(),
                page.totalElements(), page.totalPages(), page.hasNext(), page.hasPrevious());
    }

    public TenantStatus toStatusFilter(String status) {
        return status == null ? null : TenantStatus.valueOf(status);
    }

    public PageQuery toPageQuery(Integer page, Integer size) {
        return new PageQuery(page == null ? 0 : page, size == null ? 20 : size);
    }

    public PlatformOverviewResponse toResponse(PlatformOverviewResult result) {
        return new PlatformOverviewResponse(
                result.totalUsers(), result.totalOrganizers(), result.totalGroups(), result.totalMembers(),
                result.totalPayments(), result.totalReceipts(), result.totalNotifications(),
                result.totalStoredFiles(), result.totalActiveTenants(), result.totalRevenuePaise(),
                result.todaySignups(), result.todayPayments(), result.todayGroups(), result.todayNotifications(),
                result.todayStorageUploads());
    }

    public PublishAnnouncementCommand toPublishCommand(AuthenticatedUser currentUser, PublishAnnouncementRequest request) {
        return new PublishAnnouncementCommand(
                request.title(), request.message(), request.startAt(), request.endAt(),
                AnnouncementSeverity.valueOf(request.severity()), currentUser.userId().toAggregateId());
    }

    public AnnouncementResponse toResponse(AnnouncementResult result) {
        return new AnnouncementResponse(
                result.announcementId().toString(), result.title(), result.message(), result.startAt(),
                result.endAt(), result.severity().name(), result.active());
    }

    public PlatformOperationsPageResponse<AnnouncementResponse> toAnnouncementPageResponse(Page<AnnouncementResult> page) {
        return new PlatformOperationsPageResponse<>(
                page.content().stream().map(this::toResponse).toList(), page.page(), page.size(),
                page.totalElements(), page.totalPages(), page.hasNext(), page.hasPrevious());
    }

    public SendBroadcastNotificationCommand toBroadcastCommand(AuthenticatedUser currentUser, BroadcastRequest request) {
        return new SendBroadcastNotificationCommand(
                BroadcastScope.valueOf(request.scope()),
                request.tenantId() == null ? null : AggregateId.from(request.tenantId()), request.title(),
                request.message(), currentUser.userId().toAggregateId());
    }

    public BroadcastResponse toResponse(BroadcastResult result) {
        return new BroadcastResponse(result.recipientCount(), result.sentCount(), result.failedCount());
    }

    public SystemHealthResponse toResponse(SystemHealthResult result) {
        return new SystemHealthResponse(
                toComponentResponse(result.database()), toComponentResponse(result.storage()),
                toComponentResponse(result.notification()), result.uptimeSeconds(), result.javaVersion(),
                result.applicationVersion(), result.buildTimestamp(), result.usedMemoryBytes(),
                result.totalMemoryBytes(), result.maxMemoryBytes(), result.usableDiskBytes(), result.totalDiskBytes());
    }

    private SystemHealthResponse.ComponentHealthResponse toComponentResponse(ComponentHealth health) {
        return new SystemHealthResponse.ComponentHealthResponse(health.name(), health.status().name(), health.detail());
    }
}
