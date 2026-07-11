package in.bachatsetu.backend.infrastructure.auth.sms;

/** The set of pluggable SMS providers a deployment may select via {@code SMS_PROVIDER}. */
public enum SmsProviderType {
    MSG91,
    FAST2SMS,
    TWILIO
}
