package in.bachatsetu.backend.email.domain.model;

/**
 * Every email BachatSetu can send. {@code PASSWORD_RESET}, {@code PAYMENT_RECEIPT}, and
 * {@code MONTHLY_STATEMENT} are placeholders reserved for a future sprint — a template exists in
 * {@link in.bachatsetu.backend.email.domain.service.EmailTemplateCatalog} so the category is
 * already usable end-to-end, but no module publishes an event that triggers them yet.
 */
public enum EmailTemplateCategory {
    WELCOME,
    SIGNUP_COMPLETED,
    INVITATION,
    INVITATION_REVOKED,
    PASSWORD_RESET,
    PAYMENT_RECEIPT,
    MONTHLY_STATEMENT
}
