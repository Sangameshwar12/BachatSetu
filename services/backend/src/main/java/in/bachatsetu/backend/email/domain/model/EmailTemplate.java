package in.bachatsetu.backend.email.domain.model;

import java.util.Objects;

/**
 * A reusable, unrendered email template: subject, HTML body, and a plain-text fallback, each
 * containing {@code {{variable}}} placeholders. {@link
 * in.bachatsetu.backend.email.domain.service.EmailTemplateRenderer} fills them in.
 */
public record EmailTemplate(
        EmailTemplateCategory category, String subjectTemplate, String htmlTemplate, String textTemplate) {

    public EmailTemplate {
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(subjectTemplate, "subjectTemplate must not be null");
        Objects.requireNonNull(htmlTemplate, "htmlTemplate must not be null");
        Objects.requireNonNull(textTemplate, "textTemplate must not be null");
    }
}
