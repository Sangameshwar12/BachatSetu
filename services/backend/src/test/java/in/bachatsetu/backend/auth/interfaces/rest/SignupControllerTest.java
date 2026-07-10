package in.bachatsetu.backend.auth.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.signup.exception.SignupApplicationException;
import in.bachatsetu.backend.auth.application.signup.exception.SignupFailureReason;
import in.bachatsetu.backend.auth.application.signup.query.SignupCompletedResult;
import in.bachatsetu.backend.auth.application.signup.query.SignupStartedResult;
import in.bachatsetu.backend.auth.application.signup.usecase.CompleteSignupUseCase;
import in.bachatsetu.backend.auth.application.signup.usecase.StartSignupUseCase;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenValue;
import in.bachatsetu.backend.auth.application.token.port.RefreshTokenCredential;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.interfaces.rest.config.AuthenticationOpenApiConfig;
import in.bachatsetu.backend.auth.interfaces.rest.controller.SignupController;
import in.bachatsetu.backend.auth.interfaces.rest.exception.GlobalExceptionHandler;
import in.bachatsetu.backend.auth.interfaces.rest.mapper.SignupApiMapper;
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

@WebMvcTest(SignupController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({SignupApiMapper.class, GlobalExceptionHandler.class, AuthenticationOpenApiConfig.class})
class SignupControllerTest {

    private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final Instant EXPIRES_AT = Instant.parse("2026-07-09T10:05:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StartSignupUseCase startSignup;

    @MockBean
    private CompleteSignupUseCase completeSignup;

    @Test
    void startsSignupAndReturnsAcceptedWithTheOtpExpiry() throws Exception {
        when(startSignup.execute(any())).thenReturn(
                new SignupStartedResult(new UserId(USER_ID), "+919876543210", EXPIRES_AT));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.mobileNumber").value("+919876543210"))
                .andExpect(jsonPath("$.otpExpiresAt").value(EXPIRES_AT.toString()));
    }

    @Test
    void rejectsStartWithValidationProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"givenName":"","mobileNumber":"12345","preferredLanguage":"KLINGON","acceptedTerms":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @ParameterizedTest
    @MethodSource("startFailures")
    void mapsStartFailuresToProblemDetails(
            SignupFailureReason reason, int expectedStatus, String expectedCode) throws Exception {
        when(startSignup.execute(any())).thenThrow(new SignupApplicationException(reason, "safe failure"));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody()))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.detail").value("safe failure"));
    }

    @Test
    void verifiesSignupAndReturnsTokens() throws Exception {
        RefreshTokenCredential refreshToken = RefreshTokenCredential.create(RefreshTokenId.newId(), "s".repeat(32));
        when(completeSignup.execute(any())).thenReturn(new SignupCompletedResult(
                new UserId(USER_ID), AccessTokenValue.of("access-token-value"), EXPIRES_AT, refreshToken,
                EXPIRES_AT.plusSeconds(2_592_000)));

        mockMvc.perform(post("/api/v1/auth/signup/verify")
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
            SignupFailureReason reason, int expectedStatus, String expectedCode) throws Exception {
        when(completeSignup.execute(any())).thenThrow(new SignupApplicationException(reason, "safe failure"));

        mockMvc.perform(post("/api/v1/auth/signup/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyBody("482913")))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    private static Stream<Arguments> startFailures() {
        return Stream.of(
                Arguments.of(SignupFailureReason.TERMS_NOT_ACCEPTED, 400, "terms-not-accepted"),
                Arguments.of(SignupFailureReason.MOBILE_ALREADY_REGISTERED, 409, "mobile-already-registered"),
                Arguments.of(SignupFailureReason.EMAIL_ALREADY_REGISTERED, 409, "email-already-registered"));
    }

    private static Stream<Arguments> verifyFailures() {
        return Stream.of(
                Arguments.of(SignupFailureReason.OTP_EXPIRED, 410, "otp-expired"),
                Arguments.of(SignupFailureReason.OTP_INVALID, 422, "otp-invalid"),
                Arguments.of(SignupFailureReason.OTP_ATTEMPTS_EXCEEDED, 429, "otp-verification-limit-exceeded"));
    }

    private static String startBody() {
        return """
                {"givenName":"Asha","familyName":"Rao","mobileNumber":"+919876543210",
                "email":"asha@example.com","preferredLanguage":"ENGLISH","acceptedTerms":true}
                """;
    }

    private static String verifyBody(String code) {
        return """
                {"userId":"123e4567-e89b-12d3-a456-426614174000","code":"%s"}
                """.formatted(code);
    }
}
