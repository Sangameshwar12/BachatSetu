package in.bachatsetu.backend.email.domain.model;

import java.util.Objects;

/** A template already rendered with real values — what actually gets sent. */
public record EmailContent(String subject, String htmlBody, String textBody) {

    public EmailContent {
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(htmlBody, "htmlBody must not be null");
        Objects.requireNonNull(textBody, "textBody must not be null");
    }
}
