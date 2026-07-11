package in.bachatsetu.backend.email.domain.model;

import java.util.Objects;

/** One fully rendered, ready-to-send email, handed to {@link EmailAddress}'s recipient. */
public record EmailMessage(EmailAddress to, EmailTemplateCategory category, EmailContent content) {

    public EmailMessage {
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }
}
