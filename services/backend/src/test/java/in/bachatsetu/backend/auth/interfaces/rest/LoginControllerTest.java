package in.bachatsetu.backend.auth.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.login.exception.LoginApplicationException;
import in.bachatsetu.backend.auth.application.login.exception.LoginFailureReason;
import in.bachatsetu.backend.auth.application.login.query.LoginCompletedResult;
import in.bachatsetu.backend.auth.application.login.query.LoginStartedResult;
import in.bachatsetu.backend.auth.application.login.usecase.CompleteLoginUseCase;
import in.bachatsetu.backend.auth.application.login.usecase.StartLoginUseCase;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenValue;
import in.bachatsetu.backend.auth.application.token.port.RefreshTokenCredential;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.interfaces.rest.config.AuthenticationOpenApiConfig;
import in.bachatsetu.backend.auth.interfaces.rest.controller.LoginController;
import in.bachatsetu.backend.auth.interfaces.rest.exception.GlobalExceptionHandler;
import in.bachatsetu.backend.auth.interfaces.rest.mapper.LoginApiMapper;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LoginController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({LoginApiMapper.class, GlobalExceptionHandler.class, AuthenticationOpenApiConfig.class})
class LoginControllerTest {

    private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final Instant EXPIRES_AT = Instant.parse("2026-07-12T10:05:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StartLoginUseCase startLogin;

    @MockBean
    private CompleteLoginUseCase completeLogin;

    @Test
    void startsLoginAndReturnsAcceptedWithTheOtpExpiry() throws Exception {
        when(startLogin.execute(any())).thenReturn(
                new LoginStartedResult(new UserId(USER_ID), "+919876543210", EXPIRES_AT));

        mockMvc.perform(post("/api/v1/auth/login/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.mobileNumber").value("+919876543210"))
                .andExpect(jsonPath("$.otpExpiresAt").value(EXPIRES_AT.toString()));
    }

    @Test
    void rejectsStartWithValidationProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mobileNumber":"12345"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @ParameterizedTest
    @MethodSource("startFailures")
    void mapsStartFailuresToProblemDetails(
            LoginFailureReason reason, int expectedStatus, String expectedCode) throws Exception {
        when(startLogin.execute(any())).thenThrow(new LoginApplicationException(reason, "safe failure"));

        mockMvc.perform(post("/api/v1/auth/login/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody()))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.detail").value("safe failure"));
    }

    @Test
    void verifiesLoginAndReturnsTokens() throws Exception {
        RefreshTokenCredential refreshToken = RefreshTokenCredential.create(RefreshTokenId.newId(), "s".repeat(32));
        when(completeLogin.execute(any())).thenReturn(new LoginCompletedResult(
                new UserId(USER_ID), AccessTokenValue.of("access-token-value"), EXPIRES_AT, refreshToken,
                EXPIRES_AT.plusSeconds(2_592_000)));

        mockMvc.perform(post("/api/v1/auth/login/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyBody("482913")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.accessToken").value("access-token-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @ParameterizedTest
    @MethodSource("verifyFailures")
    void mapsVerifyFailuresToProblemDetails(
            LoginFailureReason reason, int expectedStatus, String expectedCode) throws Exception {
        when(completeLogin.execute(any())).thenThrow(new LoginApplicationException(reason, "safe failure"));

        mockMvc.perform(post("/api/v1/auth/login/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyBody("482913")))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    private static Stream<Arguments> startFailures() {
        return Stream.of(
                Arguments.of(LoginFailureReason.MOBILE_NOT_REGISTERED, 404, "mobile-not-registered"),
                Arguments.of(LoginFailureReason.ACCOUNT_NOT_ACTIVE, 403, "account-not-active"));
    }

    private static Stream<Arguments> verifyFailures() {
        return Stream.of(
                Arguments.of(LoginFailureReason.OTP_EXPIRED, 410, "otp-expired"),
                Arguments.of(LoginFailureReason.OTP_INVALID, 422, "otp-invalid"),
                Arguments.of(LoginFailureReason.OTP_ATTEMPTS_EXCEEDED, 429, "otp-verification-limit-exceeded"));
    }

    private static String startBody() {
        return """
                {"mobileNumber":"+919876543210"}
                """;
    }

    private static String verifyBody(String code) {
        return """
                {"userId":"123e4567-e89b-12d3-a456-426614174000","code":"%s"}
                """.formatted(code);
    }
}
