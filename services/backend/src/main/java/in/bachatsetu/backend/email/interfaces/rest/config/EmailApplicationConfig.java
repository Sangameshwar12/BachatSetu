package in.bachatsetu.backend.email.interfaces.rest.config;

import in.bachatsetu.backend.email.application.port.EmailSenderPort;
import in.bachatsetu.backend.email.application.service.SendEmailApplicationService;
import in.bachatsetu.backend.email.application.usecase.SendEmailUseCase;
import in.bachatsetu.backend.email.domain.service.EmailTemplateCatalog;
import in.bachatsetu.backend.email.domain.service.EmailTemplateRenderer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires {@link SendEmailUseCase}. Gated on {@code bachatsetu.persistence.repositories.enabled}
 * rather than {@code @Profile}, matching every other application-layer config class in this
 * codebase (see {@code NotificationApplicationConfig} for the reasoning).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class EmailApplicationConfig {

    @Bean
    EmailTemplateCatalog emailTemplateCatalog() {
        return new EmailTemplateCatalog();
    }

    @Bean
    EmailTemplateRenderer emailTemplateRenderer() {
        return new EmailTemplateRenderer();
    }

    @Bean
    SendEmailUseCase sendEmailApplicationService(
            EmailSenderPort emailSenderPort, EmailTemplateCatalog emailTemplateCatalog,
            EmailTemplateRenderer emailTemplateRenderer) {
        return new SendEmailApplicationService(emailSenderPort, emailTemplateCatalog, emailTemplateRenderer);
    }
}
