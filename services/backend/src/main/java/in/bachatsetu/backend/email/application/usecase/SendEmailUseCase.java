package in.bachatsetu.backend.email.application.usecase;

import in.bachatsetu.backend.email.application.command.SendEmailCommand;
import in.bachatsetu.backend.email.domain.model.EmailSendResult;

/**
 * Sends one templated email. Every other module depends on this interface — never on {@link
 * in.bachatsetu.backend.email.application.port.EmailSenderPort} or a provider type directly —
 * mirroring how every module depends on Audit's {@code CreateAuditEntryUseCase}.
 */
@FunctionalInterface
public interface SendEmailUseCase {

    EmailSendResult execute(SendEmailCommand command);
}
