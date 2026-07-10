package in.bachatsetu.backend.user.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.user.application.query.OnboardingCompletedResult;
import in.bachatsetu.backend.user.application.usecase.CompleteOnboardingUseCase;
import in.bachatsetu.backend.user.interfaces.rest.dto.CompleteOnboardingRequest;
import in.bachatsetu.backend.user.interfaces.rest.dto.OnboardingCompletedResponse;
import in.bachatsetu.backend.user.interfaces.rest.mapper.OnboardingApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the post-signup profile onboarding step for the currently authenticated user. */
@RestController
@RequestMapping(path = "/api/v1/users/me/onboarding", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Onboarding", description = "Completes the post-signup profile onboarding step")
@ConditionalOnProperty(
        prefix = "bachatsetu.user.rest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OnboardingController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final CompleteOnboardingUseCase completeOnboarding;
    private final CurrentUserProvider currentUserProvider;
    private final OnboardingApiMapper mapper;

    public OnboardingController(
            CompleteOnboardingUseCase completeOnboarding,
            CurrentUserProvider currentUserProvider,
            OnboardingApiMapper mapper) {
        this.completeOnboarding = completeOnboarding;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Complete profile onboarding",
            description = "Records the caller's optional photo, city, state, and notification preference.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Onboarding completed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "No profile exists for this user", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Onboarding already completed", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OnboardingCompletedResponse complete(@Valid @RequestBody CompleteOnboardingRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        OnboardingCompletedResult result = completeOnboarding.execute(mapper.toCommand(currentUser, request));
        return mapper.toResponse(result);
    }
}
