package in.bachatsetu.backend.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Email;
import in.bachatsetu.backend.shared.domain.PhoneNumber;
import in.bachatsetu.backend.user.application.command.CompleteOnboardingCommand;
import in.bachatsetu.backend.user.application.exception.OnboardingApplicationException;
import in.bachatsetu.backend.user.application.exception.OnboardingFailureReason;
import in.bachatsetu.backend.user.application.port.ClockPort;
import in.bachatsetu.backend.user.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.user.application.port.TransactionPort;
import in.bachatsetu.backend.user.application.query.OnboardingCompletedResult;
import in.bachatsetu.backend.user.domain.model.PersonName;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import in.bachatsetu.backend.user.domain.model.UserContact;
import in.bachatsetu.backend.user.domain.model.UserProfile;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompleteOnboardingApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-09T06:00:00Z");
    private static final AggregateId USER_ID = AggregateId.newId();

    private UserRepository userRepository;
    private DomainEventPublisherPort eventPublisher;
    private CompleteOnboardingApplicationService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        eventPublisher = mock(DomainEventPublisherPort.class);
        ClockPort clock = () -> NOW;
        TransactionPort transaction = java.util.function.Supplier::get;
        service = new CompleteOnboardingApplicationService(userRepository, eventPublisher, clock, transaction);
    }

    @Test
    void completesOnboardingAndPublishesEvents() {
        UserProfile profile = invitedProfile();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(profile));
        AggregateId photoFileId = AggregateId.newId();

        OnboardingCompletedResult result = service.execute(
                new CompleteOnboardingCommand(USER_ID, "Pune", "Maharashtra", photoFileId, false));

        assertThat(result.city()).isEqualTo("Pune");
        assertThat(result.state()).isEqualTo("Maharashtra");
        assertThat(result.photoFileId()).isEqualTo(photoFileId);
        assertThat(result.notificationsEnabled()).isFalse();
        verify(userRepository).save(profile);
        verify(eventPublisher).publish(any());
    }

    @Test
    void rejectsWhenNoProfileExists() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                        new CompleteOnboardingCommand(USER_ID, "Pune", "Maharashtra", null, true)))
                .isInstanceOfSatisfying(OnboardingApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(OnboardingFailureReason.PROFILE_NOT_FOUND));
    }

    @Test
    void rejectsWhenAlreadyOnboarded() {
        UserProfile profile = invitedProfile();
        profile.completeOnboarding("Pune", "Maharashtra", null, true, USER_ID, NOW);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.execute(
                        new CompleteOnboardingCommand(USER_ID, "Mumbai", "Maharashtra", null, false)))
                .isInstanceOfSatisfying(OnboardingApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(OnboardingFailureReason.ALREADY_ONBOARDED));
    }

    private UserProfile invitedProfile() {
        PersonName name = new PersonName("Asha", "Rao");
        UserContact contact = new UserContact(new Email("asha@example.com"), new PhoneNumber("+919876543210"));
        return UserProfile.register(USER_ID, name, contact, PreferredLanguage.ENGLISH, USER_ID, NOW);
    }
}
