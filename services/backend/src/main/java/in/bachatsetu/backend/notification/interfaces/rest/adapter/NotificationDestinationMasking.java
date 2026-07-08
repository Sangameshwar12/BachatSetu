package in.bachatsetu.backend.notification.interfaces.rest.adapter;

import java.util.Objects;

/** Masks a notification destination (email, phone number, or device token) before it reaches logs. */
final class NotificationDestinationMasking {

    private static final int VISIBLE_PREFIX = 2;
    private static final int VISIBLE_SUFFIX = 2;

    private NotificationDestinationMasking() {
    }

    static String mask(String destination) {
        Objects.requireNonNull(destination, "destination must not be null");
        int visible = VISIBLE_PREFIX + VISIBLE_SUFFIX;
        if (destination.length() <= visible) {
            return "*".repeat(destination.length());
        }
        String prefix = destination.substring(0, VISIBLE_PREFIX);
        String suffix = destination.substring(destination.length() - VISIBLE_SUFFIX);
        return prefix + "*".repeat(destination.length() - visible) + suffix;
    }
}
