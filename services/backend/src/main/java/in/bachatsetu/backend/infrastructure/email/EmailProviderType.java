package in.bachatsetu.backend.infrastructure.email;

/** The set of pluggable email providers a deployment may select via {@code EMAIL_PROVIDER}. */
public enum EmailProviderType {
    AWS_SES,
    RESEND,
    SENDGRID
}
