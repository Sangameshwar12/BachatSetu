package in.bachatsetu.backend.user.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Completes the post-signup onboarding step for an already-registered user. */
public record CompleteOnboardingCommand(
        AggregateId userId,
        String city,
        String state,
        AggregateId photoFileId,
        boolean notificationsEnabled) {

    public CompleteOnboardingCommand {
        Objects.requireNonNull(userId, "userId must not be null");
    }
}
