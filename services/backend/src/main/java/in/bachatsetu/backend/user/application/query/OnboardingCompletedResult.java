package in.bachatsetu.backend.user.application.query;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Confirms the caller's profile onboarding is complete. */
public record OnboardingCompletedResult(
        AggregateId userId,
        String city,
        String state,
        AggregateId photoFileId,
        boolean notificationsEnabled) {

    public OnboardingCompletedResult {
        Objects.requireNonNull(userId, "userId must not be null");
    }
}
