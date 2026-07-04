package in.bachatsetu.backend.notification.domain.model;

import java.util.Objects;

public record NotificationContent(String subject, String body) {

    public NotificationContent {
        subject = normalizeOptional(subject, 160);
        Objects.requireNonNull(body, "body must not be null");
        body = body.strip();
        if (body.isEmpty() || body.length() > 4_000) {
            throw new IllegalArgumentException("notification body length is invalid");
        }
    }

    private static String normalizeOptional(String value, int maximumLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException("notification subject is too long");
        }
        return normalized;
    }
}
