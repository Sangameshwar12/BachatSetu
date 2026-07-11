package in.bachatsetu.backend.email.application.service;

import in.bachatsetu.backend.email.application.command.SendEmailCommand;
import in.bachatsetu.backend.email.application.port.EmailSenderPort;
import in.bachatsetu.backend.email.application.usecase.SendEmailUseCase;
import in.bachatsetu.backend.email.domain.model.EmailContent;
import in.bachatsetu.backend.email.domain.model.EmailMessage;
import in.bachatsetu.backend.email.domain.model.EmailSendResult;
import in.bachatsetu.backend.email.domain.model.EmailTemplate;
import in.bachatsetu.backend.email.domain.service.EmailTemplateCatalog;
import in.bachatsetu.backend.email.domain.service.EmailTemplateRenderer;
import java.util.Objects;

/** Renders the requested template and hands the result to whichever {@link EmailSenderPort} is wired. */
public final class SendEmailApplicationService implements SendEmailUseCase {

    private final EmailSenderPort emailSender;
    private final EmailTemplateCatalog templateCatalog;
    private final EmailTemplateRenderer templateRenderer;

    public SendEmailApplicationService(
            EmailSenderPort emailSender, EmailTemplateCatalog templateCatalog, EmailTemplateRenderer templateRenderer) {
        this.emailSender = Objects.requireNonNull(emailSender, "emailSender must not be null");
        this.templateCatalog = Objects.requireNonNull(templateCatalog, "templateCatalog must not be null");
        this.templateRenderer = Objects.requireNonNull(templateRenderer, "templateRenderer must not be null");
    }

    @Override
    public EmailSendResult execute(SendEmailCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        EmailTemplate template = templateCatalog.templateFor(command.category());
        EmailContent content = templateRenderer.render(template, command.variables());
        EmailMessage message = new EmailMessage(command.to(), command.category(), content);
        return emailSender.send(message);
    }
}
