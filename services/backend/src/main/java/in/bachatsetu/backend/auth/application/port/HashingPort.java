package in.bachatsetu.backend.auth.application.port;

import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.OtpHash;

/** One-way OTP hashing and constant-time verification boundary. */
public interface HashingPort {

    OtpHash hash(OtpCode code);

    boolean matches(OtpCode candidate, OtpHash hash);
}
