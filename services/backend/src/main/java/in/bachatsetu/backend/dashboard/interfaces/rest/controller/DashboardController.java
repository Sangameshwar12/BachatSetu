package in.bachatsetu.backend.dashboard.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.dashboard.application.usecase.GetMemberDashboardUseCase;
import in.bachatsetu.backend.dashboard.application.usecase.GetOrganizerDashboardUseCase;
import in.bachatsetu.backend.dashboard.interfaces.rest.dto.MemberDashboardResponse;
import in.bachatsetu.backend.dashboard.interfaces.rest.dto.OrganizerDashboardResponse;
import in.bachatsetu.backend.dashboard.interfaces.rest.mapper.DashboardApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Composed, read-only dashboards for the currently authenticated member and organizer. */
@RestController
@RequestMapping(path = "/api/v1/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Dashboard", description = "Composed member and organizer home screens")
@ConditionalOnProperty(
        prefix = "bachatsetu.dashboard.rest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class DashboardController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final GetMemberDashboardUseCase getMemberDashboard;
    private final GetOrganizerDashboardUseCase getOrganizerDashboard;
    private final CurrentUserProvider currentUserProvider;
    private final DashboardApiMapper mapper;

    public DashboardController(
            GetMemberDashboardUseCase getMemberDashboard,
            GetOrganizerDashboardUseCase getOrganizerDashboard,
            CurrentUserProvider currentUserProvider,
            DashboardApiMapper mapper) {
        this.getMemberDashboard = getMemberDashboard;
        this.getOrganizerDashboard = getOrganizerDashboard;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @GetMapping("/member")
    @Operation(summary = "Member home screen", description =
            "Current group, upcoming installment, next draw, latest payment status, and recent notifications.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard available"),
        @ApiResponse(responseCode = "404", description = "No active group yet — show the Welcome Screen instead",
                content = @Content(mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public MemberDashboardResponse member() {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        return mapper.member(getMemberDashboard, currentUser);
    }

    @GetMapping("/organizer")
    @Operation(summary = "Organizer home screen", description =
            "Every group the caller owns: members, pending invitations, contribution progress, next draw, and quick actions.")
    @ApiResponse(responseCode = "200", description = "Dashboard available (empty groups list if the caller owns none)")
    public OrganizerDashboardResponse organizer() {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        return mapper.organizer(getOrganizerDashboard, currentUser);
    }
}
