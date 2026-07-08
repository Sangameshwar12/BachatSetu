package in.bachatsetu.backend.notification.application.usecase;

import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.query.NotificationResult;

/** Creates a notification and dispatches it synchronously over its channel. */
@FunctionalInterface
public interface CreateNotificationUseCase {

    NotificationResult execute(CreateNotificationCommand command);
}
