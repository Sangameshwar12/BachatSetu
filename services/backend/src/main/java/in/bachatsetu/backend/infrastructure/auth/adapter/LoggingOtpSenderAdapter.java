package in.bachatsetu.backend.infrastructure.auth.adapter;

import in.bachatsetu.backend.auth.application.port.OtpSenderPort;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Local-only sender that confirms generation without logging OTP credential material. */
public final class LoggingOtpSenderAdapter implements OtpSenderPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingOtpSenderAdapter.class);
    private static final int VISIBLE_DIGITS = 4;
    private static final int COUNTRY_PREFIX_LENGTH = 3;

    private final Clock clock;
    private final Supplier<UUID> correlationIdSupplier;

    public LoggingOtpSenderAdapter(Clock clock, Supplier<UUID> correlationIdSupplier) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.correlationIdSupplier = Objects.requireNonNull(
                correlationIdSupplier, "correlation ID supplier must not be null");
    }

    @Override
    public void send(UserId userId, MobileNumber mobileNumber, OtpPurpose purpose, OtpCode code) {
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(mobileNumber, "mobile number must not be null");
        Objects.requireNonNull(purpose, "OTP purpose must not be null");
        Objects.requireNonNull(code, "OTP code must not be null");
        UUID correlationId = Objects.requireNonNull(
                correlationIdSupplier.get(), "correlation ID must not be null");
        LOGGER.info(
                "OTP generated successfully. destination={} correlationId={} timestamp={}",
                mask(mobileNumber),
                correlationId,
                clock.instant());
    }

    private String mask(MobileNumber mobileNumber) {
        String value = mobileNumber.value();
        String nationalNumber = value.substring(COUNTRY_PREFIX_LENGTH);
        return value.substring(0, COUNTRY_PREFIX_LENGTH)
                + "*".repeat(nationalNumber.length() - VISIBLE_DIGITS)
                + nationalNumber.substring(nationalNumber.length() - VISIBLE_DIGITS);
    }
}
