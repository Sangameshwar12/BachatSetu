package in.bachatsetu.backend.notification.domain.service;

import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationTemplate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Centralizes the one canned message template per {@link NotificationCategory}, so message text is defined
 * once here rather than duplicated across the services that trigger notifications.
 */
public final class NotificationTemplateCatalog {

    private static final Map<NotificationCategory, NotificationTemplate> TEMPLATES = buildTemplates();

    private NotificationTemplateCatalog() {
    }

    public static NotificationTemplate templateFor(NotificationCategory category) {
        Objects.requireNonNull(category, "category must not be null");
        NotificationTemplate template = TEMPLATES.get(category);
        if (template == null) {
            throw new IllegalArgumentException("no template registered for category: " + category);
        }
        return template;
    }

    private static Map<NotificationCategory, NotificationTemplate> buildTemplates() {
        Map<NotificationCategory, NotificationTemplate> templates = new EnumMap<>(NotificationCategory.class);
        templates.put(NotificationCategory.VERIFICATION, new NotificationTemplate(
                NotificationCategory.VERIFICATION,
                "Account verification",
                "Hello {{memberName}}, please verify your account to continue using BachatSetu."));
        templates.put(NotificationCategory.PAYMENT_RECEIPT, new NotificationTemplate(
                NotificationCategory.PAYMENT_RECEIPT,
                "Payment receipt",
                "Hello {{memberName}}, your payment of {{amount}} has been recorded. "
                        + "Receipt number: {{receiptNumber}}."));
        templates.put(NotificationCategory.CONTRIBUTION_REMINDER, new NotificationTemplate(
                NotificationCategory.CONTRIBUTION_REMINDER,
                "Contribution reminder",
                "Hello {{memberName}}, your contribution of {{amount}} to {{groupName}} is due soon."));
        templates.put(NotificationCategory.GROUP_UPDATE, new NotificationTemplate(
                NotificationCategory.GROUP_UPDATE,
                "Group update",
                "Hello {{memberName}}, there is an update for your group {{groupName}}."));
        templates.put(NotificationCategory.DRAW_RESULT, new NotificationTemplate(
                NotificationCategory.DRAW_RESULT,
                "Draw result",
                "Hello {{memberName}}, the result for draw {{drawNumber}} in group {{groupName}} "
                        + "is now available."));
        templates.put(NotificationCategory.SECURITY_ALERT, new NotificationTemplate(
                NotificationCategory.SECURITY_ALERT,
                "Security alert",
                "Hello {{memberName}}, we noticed a security-relevant event on your account."));
        return Map.copyOf(templates);
    }
}
