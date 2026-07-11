package in.bachatsetu.backend.email.application.command;

import in.bachatsetu.backend.email.domain.model.EmailAddress;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import java.util.Map;
import java.util.Objects;

/** Input for {@code SendEmailUseCase}: which template to render and for whom, with template variables. */
public record SendEmailCommand(EmailAddress to, EmailTemplateCategory category, Map<String, String> variables) {

    public SendEmailCommand {
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(category, "category must not be null");
        variables = Map.copyOf(Objects.requireNonNull(variables, "variables must not be null"));
    }
}
