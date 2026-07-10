package in.bachatsetu.backend.auth.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.signup.command.CompleteSignupCommand;
import in.bachatsetu.backend.auth.application.signup.command.StartSignupCommand;
import in.bachatsetu.backend.auth.application.signup.query.SignupCompletedResult;
import in.bachatsetu.backend.auth.application.signup.query.SignupStartedResult;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.interfaces.rest.dto.SignupStartRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.SignupStartResponse;
import in.bachatsetu.backend.auth.interfaces.rest.dto.SignupVerifyRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.SignupVerifyResponse;
import in.bachatsetu.backend.shared.domain.Email;
import org.springframework.stereotype.Component;

/** Maps validated HTTP contracts to signup application commands and safe responses. */
@Component
public class SignupApiMapper {

    private static final String BEARER = "Bearer";

    public StartSignupCommand toCommand(SignupStartRequest request) {
        Email email = request.email() == null || request.email().isBlank() ? null : new Email(request.email());
        return new StartSignupCommand(
                request.givenName(),
                request.familyName(),
                MobileNumber.of(request.mobileNumber()),
                email,
                request.preferredLanguage(),
                request.acceptedTerms());
    }

    public SignupStartResponse toResponse(SignupStartedResult result) {
        return new SignupStartResponse(
                result.userId().toString(), result.mobileNumber(), result.otpExpiresAt());
    }

    public CompleteSignupCommand toCommand(SignupVerifyRequest request) {
        return new CompleteSignupCommand(UserId.from(request.userId()), OtpCode.of(request.code()));
    }

    public SignupVerifyResponse toResponse(SignupCompletedResult result) {
        return new SignupVerifyResponse(
                result.userId().toString(),
                result.accessToken().value(),
                result.accessTokenExpiresAt(),
                result.refreshToken().value(),
                result.refreshTokenExpiresAt(),
                BEARER);
    }
}
