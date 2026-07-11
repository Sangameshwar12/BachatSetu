/**
 * Generic, provider-agnostic email infrastructure (Sprint PI-2.2). Business modules depend only
 * on {@link in.bachatsetu.backend.email.application.usecase.SendEmailUseCase} — never on which
 * provider (AWS SES, Resend, SendGrid) is active, mirroring how every module depends on Audit's
 * {@code CreateAuditEntryUseCase} rather than a persistence type.
 */
package in.bachatsetu.backend.email;
