package in.bachatsetu.backend.auth.application.port;

import in.bachatsetu.backend.auth.domain.model.OtpCode;

/** Generates cryptographically secure six-digit OTP values. */
@FunctionalInterface
public interface RandomGeneratorPort {

    OtpCode generateOtp();
}
