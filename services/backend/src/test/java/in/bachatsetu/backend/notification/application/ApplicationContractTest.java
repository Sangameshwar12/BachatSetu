package in.bachatsetu.backend.notification.application;

import static in.bachatsetu.backend.notification.application.ApplicationTestFixture.createCommand;
import static in.bachatsetu.backend.notification.application.ApplicationTestFixture.directTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.exception.NotificationApplicationException;
import in.bachatsetu.backend.notification.application.exception.NotificationNotFoundException;
import in.bachatsetu.backend.notification.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.notification.application.port.EmailSender;
import in.bachatsetu.backend.notification.application.port.InAppNotificationSender;
import in.bachatsetu.backend.notification.application.port.SmsSender;
import in.bachatsetu.backend.notification.application.port.TransactionPort;
import in.bachatsetu.backend.notification.application.port.WhatsappSender;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ApplicationContractTest {

    @Test
    void commandContractsCarryRequiredContext() {
        CreateNotificationCommand create = createCommand();

        assertThat(create.placeholders()).isNotEmpty();
    }

    @Test
    void commandContractsRejectNullContext() {
        CreateNotificationCommand create = createCommand();

        assertThatThrownBy(() -> new CreateNotificationCommand(
                        null, create.recipientUserId(), create.destination(), create.channel(),
                        create.category(), create.placeholders(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateNotificationCommand(
                        create.tenantId(), null, create.destination(), create.channel(),
                        create.category(), create.placeholders(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateNotificationCommand(
                        create.tenantId(), create.recipientUserId(), null, create.channel(),
                        create.category(), create.placeholders(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateNotificationCommand(
                        create.tenantId(), create.recipientUserId(), create.destination(), null,
                        create.category(), create.placeholders(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateNotificationCommand(
                        create.tenantId(), create.recipientUserId(), create.destination(), create.channel(),
                        null, create.placeholders(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateNotificationCommand(
                        create.tenantId(), create.recipientUserId(), create.destination(), create.channel(),
                        create.category(), null, create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateNotificationCommand(
                        create.tenantId(), create.recipientUserId(), create.destination(), create.channel(),
                        create.category(), create.placeholders(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void outboundPortsExposeRequiredOperations() {
        AtomicReference<List<?>> published = new AtomicReference<>();
        DomainEventPublisherPort publisher = published::set;
        TransactionPort transaction = directTransaction();

        publisher.publish(List.of());
        assertThat(published.get()).isEmpty();
        assertThat(transaction.execute(() -> "committed")).isEqualTo("committed");
    }

    @Test
    void channelSenderPortsAreSingleMethodFunctionalAbstractions() {
        assertThat(EmailSender.class.getDeclaredMethods()).hasSize(1);
        assertThat(SmsSender.class.getDeclaredMethods()).hasSize(1);
        assertThat(WhatsappSender.class.getDeclaredMethods()).hasSize(1);
        assertThat(InAppNotificationSender.class.getDeclaredMethods()).hasSize(1);
    }

    @Test
    void useCaseAndExceptionContractsArePresent() {
        List<Class<?>> useCases = List.of(
                in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase.class,
                in.bachatsetu.backend.notification.application.usecase.GetNotificationUseCase.class,
                in.bachatsetu.backend.notification.application.usecase.ListNotificationsUseCase.class,
                in.bachatsetu.backend.notification.application.usecase.MarkNotificationDeliveredUseCase.class,
                in.bachatsetu.backend.notification.application.usecase.MarkNotificationFailedUseCase.class);

        assertThat(useCases).allMatch(Class::isInterface);
        assertThat(new NotificationApplicationException("application failure")).hasMessage("application failure");
        assertThat(new NotificationNotFoundException("missing")).isInstanceOf(NotificationApplicationException.class);
    }

    @Test
    void domainPortExposesTenantScopedLookupAndPagination() {
        Set<String> methods = Arrays.stream(in.bachatsetu.backend.notification.domain.port.NotificationRepository.class
                        .getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(methods).contains("findById", "findPage", "save");
    }
}
