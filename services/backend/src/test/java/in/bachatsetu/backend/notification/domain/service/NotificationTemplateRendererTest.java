package in.bachatsetu.backend.notification.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationTemplate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationTemplateRendererTest {

    private final NotificationTemplateRenderer renderer = new NotificationTemplateRenderer();

    @Test
    void substitutesEveryPlaceholderPresentInTheMap() {
        NotificationTemplate template = new NotificationTemplate(
                NotificationCategory.PAYMENT_RECEIPT,
                "Receipt for {{memberName}}",
                "Hello {{memberName}}, your payment of {{amount}} is confirmed. Receipt: {{receiptNumber}}.");

        NotificationContent content = renderer.render(template, Map.of(
                "memberName", "Asha",
                "amount", "INR 5,000.00",
                "receiptNumber", "RCT/20260708/1A2B3C4D"));

        assertThat(content.subject()).isEqualTo("Receipt for Asha");
        assertThat(content.body()).isEqualTo(
                "Hello Asha, your payment of INR 5,000.00 is confirmed. Receipt: RCT/20260708/1A2B3C4D.");
    }

    @Test
    void leavesPlaceholdersWithoutASuppliedValueUnchanged() {
        NotificationTemplate template = new NotificationTemplate(
                NotificationCategory.GROUP_UPDATE, null, "Hello {{memberName}}, update for {{groupName}}.");

        NotificationContent content = renderer.render(template, Map.of("memberName", "Ravi"));

        assertThat(content.subject()).isNull();
        assertThat(content.body()).isEqualTo("Hello Ravi, update for {{groupName}}.");
    }

    @Test
    void rendersWithNoPlaceholdersSupplied() {
        NotificationTemplate template = new NotificationTemplate(
                NotificationCategory.SECURITY_ALERT, "Security alert", "A security event occurred.");

        NotificationContent content = renderer.render(template, Map.of());

        assertThat(content.body()).isEqualTo("A security event occurred.");
    }

    @Test
    void rejectsNullInputs() {
        NotificationTemplate template = new NotificationTemplate(
                NotificationCategory.SECURITY_ALERT, null, "body");

        assertThatThrownBy(() -> renderer.render(null, Map.of())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> renderer.render(template, null)).isInstanceOf(NullPointerException.class);
    }
}
