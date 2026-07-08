package in.bachatsetu.backend.notification.domain.service;

import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationTemplate;
import java.util.Map;
import java.util.Objects;

/**
 * Renders a {@link NotificationTemplate} by replacing {@code {{placeholder}}} tokens with caller-supplied
 * values. Placeholders absent from the supplied map are left in the rendered text unchanged; this is a
 * deliberate, simple substitution with no template engine, per the sprint scope.
 */
public final class NotificationTemplateRenderer {

    public NotificationContent render(NotificationTemplate template, Map<String, String> placeholders) {
        Objects.requireNonNull(template, "template must not be null");
        Objects.requireNonNull(placeholders, "placeholders must not be null");
        String subject = template.subjectTemplate() == null ? null : substitute(template.subjectTemplate(), placeholders);
        String body = substitute(template.bodyTemplate(), placeholders);
        return new NotificationContent(subject, body);
    }

    private String substitute(String text, Map<String, String> placeholders) {
        String rendered = text;
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            rendered = rendered.replace("{{" + placeholder.getKey() + "}}", placeholder.getValue());
        }
        return rendered;
    }
}
