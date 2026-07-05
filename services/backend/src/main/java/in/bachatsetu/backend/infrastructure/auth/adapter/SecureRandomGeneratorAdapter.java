package in.bachatsetu.backend.infrastructure.auth.adapter;

import in.bachatsetu.backend.auth.application.port.RandomGeneratorPort;
import in.bachatsetu.backend.auth.domain.model.OtpCode;
import java.security.SecureRandom;
import java.util.Objects;

/** Cryptographically secure six-digit OTP generator. */
public final class SecureRandomGeneratorAdapter implements RandomGeneratorPort {

    private static final int FIRST_SIX_DIGIT_VALUE = 100_000;
    private static final int SIX_DIGIT_VALUE_COUNT = 900_000;

    private final SecureRandom secureRandom;

    public SecureRandomGeneratorAdapter(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secure random must not be null");
    }

    @Override
    public OtpCode generateOtp() {
        int value = FIRST_SIX_DIGIT_VALUE + secureRandom.nextInt(SIX_DIGIT_VALUE_COUNT);
        return OtpCode.of(Integer.toString(value));
    }
}
