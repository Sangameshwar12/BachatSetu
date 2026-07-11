package in.bachatsetu.backend.email.domain.service;

import in.bachatsetu.backend.email.domain.model.EmailTemplate;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import java.util.EnumMap;
import java.util.Map;

/**
 * One canned {@link EmailTemplate} per {@link EmailTemplateCategory}, mirroring {@code
 * NotificationTemplateCatalog}'s static-{@code EnumMap} approach. {@code PASSWORD_RESET}, {@code
 * PAYMENT_RECEIPT}, and {@code MONTHLY_STATEMENT} are registered here (per the sprint's
 * "future placeholder support" requirement) but nothing publishes an event for them yet.
 */
public final class EmailTemplateCatalog {

    private static final Map<EmailTemplateCategory, EmailTemplate> TEMPLATES = buildTemplates();

    public EmailTemplate templateFor(EmailTemplateCategory category) {
        EmailTemplate template = TEMPLATES.get(category);
        if (template == null) {
            throw new IllegalStateException("no email template registered for category " + category);
        }
        return template;
    }

    private static Map<EmailTemplateCategory, EmailTemplate> buildTemplates() {
        Map<EmailTemplateCategory, EmailTemplate> templates = new EnumMap<>(EmailTemplateCategory.class);
        templates.put(EmailTemplateCategory.WELCOME, new EmailTemplate(
                EmailTemplateCategory.WELCOME,
                "Welcome to BachatSetu",
                "<p>Hello{{givenNameGreeting}},</p>"
                        + "<p>Welcome to BachatSetu. Your account has been created and you're ready to start "
                        + "or join your first savings group.</p>",
                "Hello{{givenNameGreeting}},\n\n"
                        + "Welcome to BachatSetu. Your account has been created and you're ready to start "
                        + "or join your first savings group."));
        templates.put(EmailTemplateCategory.SIGNUP_COMPLETED, new EmailTemplate(
                EmailTemplateCategory.SIGNUP_COMPLETED,
                "Your BachatSetu account is ready",
                "<p>Hello{{givenNameGreeting}},</p>"
                        + "<p>Your signup is complete and your account is now active. You can log in and get "
                        + "started right away.</p>",
                "Hello{{givenNameGreeting}},\n\n"
                        + "Your signup is complete and your account is now active. You can log in and get "
                        + "started right away."));
        templates.put(EmailTemplateCategory.INVITATION, new EmailTemplate(
                EmailTemplateCategory.INVITATION,
                "Your invitation for {{groupName}} is ready",
                "<p>Hello,</p>"
                        + "<p>A new invitation was created for <strong>{{groupName}}</strong>. Share this code "
                        + "with the people you'd like to invite: <strong>{{invitationCode}}</strong></p>"
                        + "<p>Invitation link: <a href=\"{{invitationLink}}\">{{invitationLink}}</a></p>",
                "Hello,\n\n"
                        + "A new invitation was created for {{groupName}}. Share this code with the people "
                        + "you'd like to invite: {{invitationCode}}\n\n"
                        + "Invitation link: {{invitationLink}}"));
        templates.put(EmailTemplateCategory.INVITATION_REVOKED, new EmailTemplate(
                EmailTemplateCategory.INVITATION_REVOKED,
                "Your invitation for {{groupName}} was revoked",
                "<p>Hello,</p>"
                        + "<p>The invitation for <strong>{{groupName}}</strong> has been revoked and can no "
                        + "longer be used to join the group.</p>",
                "Hello,\n\n"
                        + "The invitation for {{groupName}} has been revoked and can no longer be used to "
                        + "join the group."));
        templates.put(EmailTemplateCategory.PASSWORD_RESET, new EmailTemplate(
                EmailTemplateCategory.PASSWORD_RESET,
                "Reset your BachatSetu password",
                "<p>Hello,</p><p>Use this link to reset your password: "
                        + "<a href=\"{{resetLink}}\">{{resetLink}}</a></p>",
                "Hello,\n\nUse this link to reset your password: {{resetLink}}"));
        templates.put(EmailTemplateCategory.PAYMENT_RECEIPT, new EmailTemplate(
                EmailTemplateCategory.PAYMENT_RECEIPT,
                "Your BachatSetu payment receipt",
                "<p>Hello,</p><p>Your payment of {{amount}} for {{groupName}} has been recorded. "
                        + "Receipt: {{receiptNumber}}</p>",
                "Hello,\n\nYour payment of {{amount}} for {{groupName}} has been recorded. "
                        + "Receipt: {{receiptNumber}}"));
        templates.put(EmailTemplateCategory.MONTHLY_STATEMENT, new EmailTemplate(
                EmailTemplateCategory.MONTHLY_STATEMENT,
                "Your BachatSetu monthly statement for {{month}}",
                "<p>Hello,</p><p>Your monthly statement for {{groupName}} ({{month}}) is ready.</p>",
                "Hello,\n\nYour monthly statement for {{groupName}} ({{month}}) is ready."));
        return templates;
    }
}
