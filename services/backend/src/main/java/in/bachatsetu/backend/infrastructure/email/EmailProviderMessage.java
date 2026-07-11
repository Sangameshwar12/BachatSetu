package in.bachatsetu.backend.infrastructure.email;

import java.util.Objects;

/** One outbound email, already rendered, in the shape every provider client sends. Never logged in full. */
public record EmailProviderMessage(
        String to, String from, String replyTo, String subject, String htmlBody, String textBody) {

    public EmailProviderMessage {
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(replyTo, "replyTo must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(htmlBody, "htmlBody must not be null");
        Objects.requireNonNull(textBody, "textBody must not be null");
    }
}
