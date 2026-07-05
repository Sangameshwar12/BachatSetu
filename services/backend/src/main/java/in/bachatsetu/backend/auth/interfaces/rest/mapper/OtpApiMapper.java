package in.bachatsetu.backend.auth.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.command.GenerateOtpCommand;
import in.bachatsetu.backend.auth.application.command.InvalidateOtpCommand;
import in.bachatsetu.backend.auth.application.command.ResendOtpCommand;
import in.bachatsetu.backend.auth.application.command.VerifyOtpCommand;
import in.bachatsetu.backend.auth.application.event.OtpExpired;
import in.bachatsetu.backend.auth.application.event.OtpRejected;
import in.bachatsetu.backend.auth.application.event.OtpRejectionReason;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;
import in.bachatsetu.backend.auth.application.query.OtpChallengeView;
import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.interfaces.rest.dto.OtpInvalidateRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.OtpRequestRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.OtpRequestResponse;
import in.bachatsetu.backend.auth.interfaces.rest.dto.OtpResendRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.OtpVerifyRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.OtpVerifyResponse;
import in.bachatsetu.backend.auth.interfaces.rest.exception.OtpRestException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Maps validated HTTP contracts to application commands and safe responses. */
@Component
public class OtpApiMapper {

    public GenerateOtpCommand toCommand(OtpRequestRequest request) {
        UserId userId = UserId.from(request.userId());
        return new GenerateOtpCommand(userId, OtpPurpose.valueOf(request.purpose()), userId.toAggregateId());
    }

    public VerifyOtpCommand toCommand(OtpVerifyRequest request) {
        UserId userId = UserId.from(request.userId());
        return new VerifyOtpCommand(
                userId,
                OtpPurpose.valueOf(request.purpose()),
                OtpCode.of(request.code()),
                userId.toAggregateId());
    }

    public ResendOtpCommand toCommand(OtpResendRequest request) {
        UserId userId = UserId.from(request.userId());
        return new ResendOtpCommand(userId, OtpPurpose.valueOf(request.purpose()), userId.toAggregateId());
    }

    public InvalidateOtpCommand toCommand(OtpInvalidateRequest request) {
        UserId userId = UserId.from(request.userId());
        return new InvalidateOtpCommand(userId, OtpPurpose.valueOf(request.purpose()), userId.toAggregateId());
    }

    public OtpRequestResponse toRequestResponse(OtpActionResult result) {
        OtpChallengeView challenge = result.challenge();
        return new OtpRequestResponse(
                challenge.verificationId().toString(),
                challenge.purpose().name(),
                challenge.status().name(),
                challenge.expiresAt(),
                challenge.resendCount());
    }

    public OtpVerifyResponse toVerifyResponse(OtpActionResult result) {
        result.events().stream()
                .filter(OtpExpired.class::isInstance)
                .findFirst()
                .ifPresent(event -> {
                    throw failure(
                            HttpStatus.GONE,
                            "otp-expired",
                            "The OTP challenge has expired.");
                });
        result.events().stream()
                .filter(OtpRejected.class::isInstance)
                .map(OtpRejected.class::cast)
                .findFirst()
                .ifPresent(this::reject);
        OtpChallengeView challenge = result.challenge();
        return new OtpVerifyResponse(
                challenge.verificationId().toString(),
                challenge.status().name(),
                true,
                challenge.verificationAttempts());
    }

    private void reject(OtpRejected rejected) {
        if (rejected.reason() == OtpRejectionReason.ATTEMPT_LIMIT) {
            throw failure(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "otp-verification-limit-exceeded",
                    "The maximum OTP verification attempts have been exceeded.");
        }
        throw failure(HttpStatus.UNPROCESSABLE_ENTITY, "otp-invalid", "The supplied OTP is invalid.");
    }

    private OtpRestException failure(HttpStatus status, String code, String message) {
        return new OtpRestException(status, URI.create("urn:bachatsetu:problem:" + code), code, message);
    }
}
