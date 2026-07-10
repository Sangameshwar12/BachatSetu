package in.bachatsetu.backend.user.application.service;

import in.bachatsetu.backend.user.application.command.CompleteOnboardingCommand;
import in.bachatsetu.backend.user.application.exception.OnboardingApplicationException;
import in.bachatsetu.backend.user.application.exception.OnboardingFailureReason;
import in.bachatsetu.backend.user.application.port.ClockPort;
import in.bachatsetu.backend.user.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.user.application.port.TransactionPort;
import in.bachatsetu.backend.user.application.query.OnboardingCompletedResult;
import in.bachatsetu.backend.user.application.usecase.CompleteOnboardingUseCase;
import in.bachatsetu.backend.user.domain.model.UserProfile;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import java.util.Objects;

/** Completes the post-signup onboarding step exactly once for an already-registered user. */
public final class CompleteOnboardingApplicationService implements CompleteOnboardingUseCase {

    private final UserRepository userRepository;
    private final DomainEventPublisherPort eventPublisher;
    private final ClockPort clock;
    private final TransactionPort transaction;

    public CompleteOnboardingApplicationService(
            UserRepository userRepository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
    }

    @Override
    public OnboardingCompletedResult execute(CompleteOnboardingCommand command) {
        Objects.requireNonNull(command, "complete onboarding command must not be null");
        return transaction.execute(() -> {
            UserProfile profile = userRepository.findById(command.userId())
                    .orElseThrow(() -> new OnboardingApplicationException(
                            OnboardingFailureReason.PROFILE_NOT_FOUND, "no profile exists for this user"));
            if (profile.onboarded()) {
                throw new OnboardingApplicationException(
                        OnboardingFailureReason.ALREADY_ONBOARDED, "profile onboarding is already complete");
            }
            profile.completeOnboarding(
                    command.city(), command.state(), command.photoFileId(), command.notificationsEnabled(),
                    command.userId(), clock.now());
            userRepository.save(profile);
            eventPublisher.publish(profile.pullDomainEvents());
            return new OnboardingCompletedResult(
                    command.userId(), profile.city(), profile.state(), profile.photoFileId(),
                    profile.notificationsEnabled());
        });
    }
}
