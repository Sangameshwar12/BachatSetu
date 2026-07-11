package in.bachatsetu.backend.email.interfaces.rest.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.auth.domain.event.UserRegistered;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.email.application.command.SendEmailCommand;
import in.bachatsetu.backend.email.application.usecase.SendEmailUseCase;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import in.bachatsetu.backend.shared.domain.Email;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WelcomeEmailListenerTest {

    private SendEmailUseCase sendEmail;
    private WelcomeEmailListener listener;

    @BeforeEach
    void setUp() {
        sendEmail = mock(SendEmailUseCase.class);
        listener = new WelcomeEmailListener(sendEmail);
    }

    @Test
    void sendsAWelcomeEmailToTheAddressCarriedByTheEvent() {
        UserRegistered event = new UserRegistered(
                UUID.randomUUID(), UserId.newId(), new Email("new-user@example.com"),
                MobileNumber.of("+919876543210"), Instant.now());

        listener.onUserRegistered(event);

        ArgumentCaptor<SendEmailCommand> captor = ArgumentCaptor.forClass(SendEmailCommand.class);
        verify(sendEmail).execute(captor.capture());
        assertThat(captor.getValue().to().value()).isEqualTo("new-user@example.com");
        assertThat(captor.getValue().category()).isEqualTo(EmailTemplateCategory.WELCOME);
    }

    @Test
    void swallowsAnEmailSendFailureWithoutPropagating() {
        UserRegistered event = new UserRegistered(
                UUID.randomUUID(), UserId.newId(), new Email("new-user@example.com"),
                MobileNumber.of("+919876543210"), Instant.now());
        when(sendEmail.execute(any())).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> listener.onUserRegistered(event)).doesNotThrowAnyException();
    }
}
