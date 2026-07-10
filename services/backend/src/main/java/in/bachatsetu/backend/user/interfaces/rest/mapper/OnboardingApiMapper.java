package in.bachatsetu.backend.user.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.user.application.command.CompleteOnboardingCommand;
import in.bachatsetu.backend.user.application.query.OnboardingCompletedResult;
import in.bachatsetu.backend.user.interfaces.rest.dto.CompleteOnboardingRequest;
import in.bachatsetu.backend.user.interfaces.rest.dto.OnboardingCompletedResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Maps validated HTTP contracts to onboarding application commands and safe responses. */
@Component
public class OnboardingApiMapper {

    public CompleteOnboardingCommand toCommand(AuthenticatedUser currentUser, CompleteOnboardingRequest request) {
        AggregateId photoFileId = request.photoFileId() == null || request.photoFileId().isBlank()
                ? null
                : new AggregateId(UUID.fromString(request.photoFileId()));
        return new CompleteOnboardingCommand(
                currentUser.userId().toAggregateId(), blankToNull(request.city()), blankToNull(request.state()),
                photoFileId, request.notificationsEnabled());
    }

    public OnboardingCompletedResponse toResponse(OnboardingCompletedResult result) {
        return new OnboardingCompletedResponse(
                result.userId().toString(),
                result.city(),
                result.state(),
                result.photoFileId() == null ? null : result.photoFileId().toString(),
                result.notificationsEnabled());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
