package in.bachatsetu.backend.auth.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.login.command.CompleteLoginCommand;
import in.bachatsetu.backend.auth.application.login.command.StartLoginCommand;
import in.bachatsetu.backend.auth.application.login.query.LoginCompletedResult;
import in.bachatsetu.backend.auth.application.login.query.LoginStartedResult;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.interfaces.rest.dto.LoginStartRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.LoginStartResponse;
import in.bachatsetu.backend.auth.interfaces.rest.dto.LoginVerifyRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.LoginVerifyResponse;
import org.springframework.stereotype.Component;

/** Maps validated HTTP contracts to login application commands and safe responses. */
@Component
public class LoginApiMapper {

    private static final String BEARER = "Bearer";

    public StartLoginCommand toCommand(LoginStartRequest request) {
        return new StartLoginCommand(MobileNumber.of(request.mobileNumber()));
    }

    public LoginStartResponse toResponse(LoginStartedResult result) {
        return new LoginStartResponse(
                result.userId().toString(), result.mobileNumber(), result.otpExpiresAt());
    }

    public CompleteLoginCommand toCommand(LoginVerifyRequest request) {
        return new CompleteLoginCommand(UserId.from(request.userId()), OtpCode.of(request.code()));
    }

    public LoginVerifyResponse toResponse(LoginCompletedResult result) {
        return new LoginVerifyResponse(
                result.userId().toString(),
                result.accessToken().value(),
                result.accessTokenExpiresAt(),
                result.refreshToken().value(),
                result.refreshTokenExpiresAt(),
                BEARER);
    }
}
