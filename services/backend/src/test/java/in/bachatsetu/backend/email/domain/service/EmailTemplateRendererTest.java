package in.bachatsetu.backend.email.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.email.domain.model.EmailContent;
import in.bachatsetu.backend.email.domain.model.EmailTemplate;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmailTemplateRendererTest {

    private final EmailTemplateRenderer renderer = new EmailTemplateRenderer();

    @Test
    void substitutesEveryPlaceholderInSubjectHtmlAndText() {
        EmailTemplate template = new EmailTemplate(
                EmailTemplateCategory.INVITATION,
                "Invite for {{groupName}}",
                "<p>{{groupName}}: {{invitationCode}}</p>",
                "{{groupName}}: {{invitationCode}}");

        EmailContent content = renderer.render(
                template, Map.of("groupName", "Diwali Bachat", "invitationCode", "ABC123"));

        assertThat(content.subject()).isEqualTo("Invite for Diwali Bachat");
        assertThat(content.htmlBody()).isEqualTo("<p>Diwali Bachat: ABC123</p>");
        assertThat(content.textBody()).isEqualTo("Diwali Bachat: ABC123");
    }

    @Test
    void replacesAMissingVariableWithAnEmptyStringRatherThanLeavingThePlaceholder() {
        EmailTemplate template = new EmailTemplate(
                EmailTemplateCategory.WELCOME, "Hello{{givenNameGreeting}}", "<p>hi{{givenNameGreeting}}</p>", "hi{{givenNameGreeting}}");

        EmailContent content = renderer.render(template, Map.of());

        assertThat(content.subject()).isEqualTo("Hello");
        assertThat(content.htmlBody()).isEqualTo("<p>hi</p>");
    }

    @Test
    void leavesATemplateWithNoPlaceholdersUnchanged() {
        EmailTemplate template = new EmailTemplate(
                EmailTemplateCategory.SIGNUP_COMPLETED, "Fixed subject", "<p>fixed body</p>", "fixed body");

        EmailContent content = renderer.render(template, Map.of("unused", "value"));

        assertThat(content.subject()).isEqualTo("Fixed subject");
        assertThat(content.htmlBody()).isEqualTo("<p>fixed body</p>");
    }

    @Test
    void toleratesAnUnterminatedPlaceholderByCopyingTheRemainderVerbatim() {
        EmailTemplate template = new EmailTemplate(
                EmailTemplateCategory.WELCOME, "Hello {{unterminated", "body", "body");

        EmailContent content = renderer.render(template, Map.of());

        assertThat(content.subject()).isEqualTo("Hello {{unterminated");
    }
}
