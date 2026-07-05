package in.bachatsetu.backend.infrastructure.auth.adapter;

import in.bachatsetu.backend.auth.application.port.HashingPort;
import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.OtpHash;
import java.util.Objects;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** BCrypt implementation of one-way OTP hashing and verification. */
public final class BCryptHashingAdapter implements HashingPort {

    private final BCryptPasswordEncoder encoder;

    public BCryptHashingAdapter(BCryptPasswordEncoder encoder) {
        this.encoder = Objects.requireNonNull(encoder, "BCrypt encoder must not be null");
    }

    @Override
    public OtpHash hash(OtpCode code) {
        Objects.requireNonNull(code, "OTP code must not be null");
        return OtpHash.encoded(encoder.encode(code.value()));
    }

    @Override
    public boolean matches(OtpCode candidate, OtpHash hash) {
        Objects.requireNonNull(candidate, "candidate OTP code must not be null");
        Objects.requireNonNull(hash, "OTP hash must not be null");
        return encoder.matches(candidate.value(), hash.value());
    }
}
