/**
 * Real email provider integrations (AWS SES, Resend, SendGrid) and the retry/metrics/health
 * orchestration wrapping them, mirroring {@code infrastructure.auth.sms}'s SMS provider
 * integration exactly. Nothing outside this package (and {@link
 * in.bachatsetu.backend.email.application.port.EmailSenderPort}'s other implementations) ever
 * knows which provider is active.
 */
package in.bachatsetu.backend.infrastructure.email;
