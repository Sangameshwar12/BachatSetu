package in.bachatsetu.backend.auth.application.port;

import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;

/** Delivers an ephemeral OTP without retaining it. */
@FunctionalInterface
public interface OtpSenderPort {

    void send(UserId userId, MobileNumber mobileNumber, OtpPurpose purpose, OtpCode code);
}
