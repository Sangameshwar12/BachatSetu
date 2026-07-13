package in.bachatsetu.backend.infrastructure.auth.adapter;

import in.bachatsetu.backend.auth.application.port.RandomGeneratorPort;
import in.bachatsetu.backend.auth.domain.model.OtpCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TEMPORARY MVP TEST OTP — REMOVE BEFORE PRODUCTION
// Returns the fixed code below instead of a random one, for MVP/demo environments only. Wired in
// only when bachatsetu.authentication.otp.test-mode (AUTH_OTP_TEST_MODE) is true; see
// AuthenticationInfrastructureConfig, which guarantees this and SecureRandomGeneratorAdapter are
// never both active at once.
public final class FixedTestOtpGeneratorAdapter implements RandomGeneratorPort {

    private static final Logger LOG = LoggerFactory.getLogger(FixedTestOtpGeneratorAdapter.class);

    // TEMPORARY MVP TEST OTP — REMOVE BEFORE PRODUCTION
    private static final String FIXED_TEST_OTP = "102030";

    // TEMPORARY MVP TEST OTP — REMOVE BEFORE PRODUCTION
    public FixedTestOtpGeneratorAdapter() {
        LOG.warn(
                """
                *************************************************
                TEST OTP MODE ENABLED
                ALL USERS WILL USE OTP: 102030
                REMOVE BEFORE PRODUCTION
                *************************************************""");
    }

    // TEMPORARY MVP TEST OTP — REMOVE BEFORE PRODUCTION
    @Override
    public OtpCode generateOtp() {
        return OtpCode.of(FIXED_TEST_OTP);
    }
}
