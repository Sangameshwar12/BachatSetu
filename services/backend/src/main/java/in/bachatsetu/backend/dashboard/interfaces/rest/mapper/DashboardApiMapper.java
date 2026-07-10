package in.bachatsetu.backend.dashboard.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.dashboard.application.query.MemberDashboardResult;
import in.bachatsetu.backend.dashboard.application.query.NextDrawSummary;
import in.bachatsetu.backend.dashboard.application.query.OrganizerDashboardResult;
import in.bachatsetu.backend.dashboard.application.query.OrganizerGroupSummary;
import in.bachatsetu.backend.dashboard.application.usecase.GetMemberDashboardUseCase;
import in.bachatsetu.backend.dashboard.application.usecase.GetOrganizerDashboardUseCase;
import in.bachatsetu.backend.dashboard.interfaces.rest.dto.MemberDashboardResponse;
import in.bachatsetu.backend.dashboard.interfaces.rest.dto.MemberDashboardResponse.CurrentGroupResponse;
import in.bachatsetu.backend.dashboard.interfaces.rest.dto.MemberDashboardResponse.NextDrawResponse;
import in.bachatsetu.backend.dashboard.interfaces.rest.dto.MemberDashboardResponse.NotificationSummaryResponse;
import in.bachatsetu.backend.dashboard.interfaces.rest.dto.OrganizerDashboardResponse;
import in.bachatsetu.backend.dashboard.interfaces.rest.dto.OrganizerDashboardResponse.OrganizerGroupResponse;
import in.bachatsetu.backend.dashboard.interfaces.rest.dto.OrganizerDashboardResponse.QuickActionResponse;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps composed dashboard application results to safe REST responses. */
@Component
public class DashboardApiMapper {

    public MemberDashboardResponse member(GetMemberDashboardUseCase useCase, AuthenticatedUser currentUser) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        MemberDashboardResult result =
                useCase.execute(currentUser.tenantId(), currentUser.userId().toAggregateId());
        return toResponse(result);
    }

    public OrganizerDashboardResponse organizer(GetOrganizerDashboardUseCase useCase, AuthenticatedUser currentUser) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        OrganizerDashboardResult result =
                useCase.execute(currentUser.tenantId(), currentUser.userId().toAggregateId());
        return toResponse(result);
    }

    private MemberDashboardResponse toResponse(MemberDashboardResult result) {
        CurrentGroupResponse currentGroup = new CurrentGroupResponse(
                result.currentGroup().groupId().toString(), result.currentGroup().groupCode(),
                result.currentGroup().name(), result.currentGroup().upcomingInstallmentAmountPaise(),
                result.currentGroup().currencyCode(), result.currentGroup().frequency(),
                result.currentGroup().memberCount(), result.currentGroup().maximumMembers());
        NextDrawResponse nextDraw = toResponse(result.nextDraw());
        return new MemberDashboardResponse(
                currentGroup, nextDraw, result.latestPaymentStatus(),
                result.recentNotifications().stream()
                        .map(notification -> new NotificationSummaryResponse(
                                notification.notificationId().toString(), notification.category(),
                                notification.status(), notification.createdAt()))
                        .toList());
    }

    private OrganizerDashboardResponse toResponse(OrganizerDashboardResult result) {
        return new OrganizerDashboardResponse(
                result.groups().stream().map(this::toResponse).toList(),
                result.quickActions().stream()
                        .map(action -> new QuickActionResponse(action.label(), action.method(), action.path()))
                        .toList());
    }

    private OrganizerGroupResponse toResponse(OrganizerGroupSummary summary) {
        return new OrganizerGroupResponse(
                summary.groupId().toString(), summary.groupCode(), summary.name(), summary.memberCount(),
                summary.maximumMembers(), summary.hasActiveInvitation(), toResponse(summary.nextDraw()),
                summary.contributionProgressPercent());
    }

    private NextDrawResponse toResponse(NextDrawSummary summary) {
        return summary == null
                ? null
                : new NextDrawResponse(summary.drawId().toString(), summary.scheduledAt(), summary.status());
    }
}
