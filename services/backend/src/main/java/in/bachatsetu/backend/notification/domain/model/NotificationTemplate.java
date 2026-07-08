package in.bachatsetu.backend.notification.domain.model;

import java.util.Objects;

/** A named, category-scoped message template containing {{placeholder}} tokens. */
public record NotificationTemplate(NotificationCategory category, String subjectTemplate, String bodyTemplate) {

    public NotificationTemplate {
        Objects.requireNonNull(category, "category must not be null");
        subjectTemplate = subjectTemplate == null ? null : subjectTemplate.strip();
        Objects.requireNonNull(bodyTemplate, "bodyTemplate must not be null");
        bodyTemplate = bodyTemplate.strip();
        if (bodyTemplate.isEmpty()) {
            throw new IllegalArgumentException("bodyTemplate must not be blank");
        }
    }
}
