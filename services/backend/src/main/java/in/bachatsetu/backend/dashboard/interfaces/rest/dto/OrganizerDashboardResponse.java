package in.bachatsetu.backend.dashboard.interfaces.rest.dto;

import in.bachatsetu.backend.dashboard.interfaces.rest.dto.MemberDashboardResponse.NextDrawResponse;
import java.util.List;

public record OrganizerDashboardResponse(List<OrganizerGroupResponse> groups, List<QuickActionResponse> quickActions) {

    public OrganizerDashboardResponse {
        groups = List.copyOf(groups);
        quickActions = List.copyOf(quickActions);
    }

    public record OrganizerGroupResponse(
            String groupId,
            String groupCode,
            String name,
            int memberCount,
            int maximumMembers,
            boolean hasActiveInvitation,
            NextDrawResponse nextDraw,
            int contributionProgressPercent) {
    }

    public record QuickActionResponse(String label, String method, String path) {
    }
}
