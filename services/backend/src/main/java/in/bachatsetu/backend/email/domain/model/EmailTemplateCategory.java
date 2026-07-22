package in.bachatsetu.backend.email.domain.model;

/**
 * Every email BachatSetu can send. {@code PASSWORD_RESET}, {@code PAYMENT_RECEIPT}, and
 * {@code MONTHLY_STATEMENT} are placeholders reserved for a future sprint — a template exists in
 * {@link in.bachatsetu.backend.email.domain.service.EmailTemplateCatalog} so the category is
 * already usable end-to-end, but no module publishes an event that triggers them yet.
 *
 * <p>{@code GENERAL_NOTIFICATION} is different from the others: it tags an email that arrives
 * already fully rendered by the {@code notification} module (group reminders, payment
 * confirmations, draw results, ...) rather than one selected from {@link
 * in.bachatsetu.backend.email.domain.service.EmailTemplateCatalog} — it exists purely as a
 * {@code provider}/audit tag on {@link EmailMessage} and is never looked up in the catalog.
 */
public enum EmailTemplateCategory {
    WELCOME,
    SIGNUP_COMPLETED,
    INVITATION,
    INVITATION_REVOKED,
    PASSWORD_RESET,
    PAYMENT_RECEIPT,
    MONTHLY_STATEMENT,
    GENERAL_NOTIFICATION
}
