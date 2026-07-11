package in.bachatsetu.backend.email.interfaces.rest.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.auth.domain.event.UserActivated;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.email.application.command.SendEmailCommand;
import in.bachatsetu.backend.email.application.usecase.SendEmailUseCase;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import in.bachatsetu.backend.shared.domain.Email;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SignupCompletedEmailListenerTest {

    private SendEmailUseCase sendEmail;
    private UserRepository userRepository;
    private SignupCompletedEmailListener listener;

    @BeforeEach
    void setUp() {
        sendEmail = mock(SendEmailUseCase.class);
        userRepository = mock(UserRepository.class);
        listener = new SignupCompletedEmailListener(sendEmail, userRepository);
    }

    @Test
    void sendsASignupCompletedEmailToTheActivatedUsersAddress() {
        UserId userId = UserId.newId();
        User user = mock(User.class);
        when(user.email()).thenReturn(new Email("activated@example.com"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        UserActivated event = new UserActivated(UUID.randomUUID(), userId, Instant.now());

        listener.onUserActivated(event);

        ArgumentCaptor<SendEmailCommand> captor = ArgumentCaptor.forClass(SendEmailCommand.class);
        verify(sendEmail).execute(captor.capture());
        assertThat(captor.getValue().to().value()).isEqualTo("activated@example.com");
        assertThat(captor.getValue().category()).isEqualTo(EmailTemplateCategory.SIGNUP_COMPLETED);
    }

    @Test
    void doesNothingWhenTheUserCannotBeFound() {
        UserId userId = UserId.newId();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        UserActivated event = new UserActivated(UUID.randomUUID(), userId, Instant.now());

        listener.onUserActivated(event);

        verifyNoInteractions(sendEmail);
    }

    @Test
    void swallowsAnEmailSendFailureWithoutPropagating() {
        UserId userId = UserId.newId();
        User user = mock(User.class);
        when(user.email()).thenReturn(new Email("activated@example.com"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sendEmail.execute(any())).thenThrow(new RuntimeException("boom"));
        UserActivated event = new UserActivated(UUID.randomUUID(), userId, Instant.now());

        assertThatCode(() -> listener.onUserActivated(event)).doesNotThrowAnyException();
    }
}
