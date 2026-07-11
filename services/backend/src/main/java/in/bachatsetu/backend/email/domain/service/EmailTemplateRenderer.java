package in.bachatsetu.backend.email.domain.service;

import in.bachatsetu.backend.email.domain.model.EmailContent;
import in.bachatsetu.backend.email.domain.model.EmailTemplate;
import java.util.Map;
import java.util.Objects;

/**
 * Renders an {@link EmailTemplate} by substituting {@code {{key}}} placeholders with caller-supplied
 * values — no template engine, mirroring {@code NotificationTemplateRenderer}'s own approach so this
 * codebase keeps exactly one templating convention. A placeholder with no matching variable is
 * replaced with an empty string rather than left in the output, since not every template uses every
 * variable (for example {@code WELCOME} has no {@code {{givenName}}} available at registration time).
 */
public final class EmailTemplateRenderer {

    public EmailContent render(EmailTemplate template, Map<String, String> variables) {
        Objects.requireNonNull(template, "template must not be null");
        Objects.requireNonNull(variables, "variables must not be null");
        return new EmailContent(
                substitute(template.subjectTemplate(), variables),
                substitute(template.htmlTemplate(), variables),
                substitute(template.textTemplate(), variables));
    }

    private String substitute(String template, Map<String, String> variables) {
        StringBuilder result = new StringBuilder(template.length());
        int index = 0;
        while (index < template.length()) {
            int start = template.indexOf("{{", index);
            if (start < 0) {
                result.append(template, index, template.length());
                break;
            }
            int end = template.indexOf("}}", start);
            if (end < 0) {
                result.append(template, index, template.length());
                break;
            }
            result.append(template, index, start);
            String key = template.substring(start + 2, end).trim();
            result.append(variables.getOrDefault(key, ""));
            index = end + 2;
        }
        return result.toString();
    }
}
