package in.bachatsetu.backend.auth.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.event.OtpExpired;
import in.bachatsetu.backend.auth.application.event.OtpRejected;
import in.bachatsetu.backend.auth.application.event.OtpRejectionReason;
import in.bachatsetu.backend.auth.application.exception.OtpApplicationException;
import in.bachatsetu.backend.auth.application.exception.OtpFailureReason;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;
import in.bachatsetu.backend.auth.application.query.OtpChallengeView;
import in.bachatsetu.backend.auth.application.usecase.GenerateOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.InvalidateOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.ResendOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.VerifyOtpUseCase;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpStatus;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.interfaces.rest.config.AuthenticationOpenApiConfig;
import in.bachatsetu.backend.auth.interfaces.rest.controller.AuthenticationController;
import in.bachatsetu.backend.auth.interfaces.rest.exception.GlobalExceptionHandler;
import in.bachatsetu.backend.auth.interfaces.rest.mapper.OtpApiMapper;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({OtpApiMapper.class, GlobalExceptionHandler.class, AuthenticationOpenApiConfig.class})
class AuthenticationControllerTest {

    private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID VERIFICATION_ID = UUID.fromString("9b7ee98d-4935-4d24-aafa-58048e559f1d");
    private static final Instant EXPIRES_AT = Instant.parse("2026-07-05T10:05:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GenerateOtpUseCase generateOtp;

    @MockBean
    private VerifyOtpUseCase verifyOtp;

    @MockBean
    private ResendOtpUseCase resendOtp;

    @MockBean
    private InvalidateOtpUseCase invalidateOtp;

    @Test
    void requestsOtp() throws Exception {
        when(generateOtp.generate(any())).thenReturn(result(OtpStatus.PENDING, 0, 0));

        mockMvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.verificationId").value(VERIFICATION_ID.toString()))
                .andExpect(jsonPath("$.purpose").value("SIGN_IN"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.expiresAt").value(EXPIRES_AT.toString()))
                .andExpect(jsonPath("$.resendCount").value(0));
    }

    @Test
    void verifiesOtp() throws Exception {
        when(verifyOtp.verify(any())).thenReturn(result(OtpStatus.VERIFIED, 1, 0));

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyBody("482913")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationId").value(VERIFICATION_ID.toString()))
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.verificationAttempts").value(1));
    }

    @Test
    void resendsOtp() throws Exception {
        when(resendOtp.resend(any())).thenReturn(result(OtpStatus.PENDING, 0, 1));

        mockMvc.perform(post("/api/v1/auth/otp/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.resendCount").value(1));
    }

    @Test
    void invalidatesOtp() throws Exception {
        when(invalidateOtp.invalidate(any())).thenReturn(result(OtpStatus.INVALIDATED, 0, 0));

        mockMvc.perform(post("/api/v1/auth/otp/invalidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALIDATED"));
    }

    @Test
    void rejectsInvalidRequestWithProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyBody("12ab")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:bachatsetu:problem:validation-error"))
                .andExpect(jsonPath("$.title").value("Request validation failed"))
                .andExpect(jsonPath("$.code").value("validation-error"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.violations[0].field").value("code"));
    }

    @Test
    void rejectsMalformedRequestWithProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:bachatsetu:problem:malformed-request"))
                .andExpect(jsonPath("$.instance").value("/api/v1/auth/otp/request"));
    }

    @ParameterizedTest
    @MethodSource("applicationFailures")
    void mapsApplicationFailures(
            OtpFailureReason reason,
            int expectedStatus,
            String expectedCode) throws Exception {
        when(generateOtp.generate(any())).thenThrow(new OtpApplicationException(reason, "safe failure"));

        mockMvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.detail").value("safe failure"));
    }

    @Test
    void mapsExpiredOtpResult() throws Exception {
        OtpActionResult expired = new OtpActionResult(
                view(OtpStatus.EXPIRED, 0, 0),
                List.of(new OtpExpired(
                        UUID.randomUUID(),
                        new AggregateId(VERIFICATION_ID),
                        new UserId(USER_ID),
                        OtpPurpose.SIGN_IN,
                        Instant.parse("2026-07-05T10:06:00Z"))));
        when(verifyOtp.verify(any())).thenReturn(expired);

        assertVerifyProblem("482913", 410, "otp-expired");
    }

    @ParameterizedTest
    @MethodSource("rejections")
    void mapsRejectedOtpResult(
            OtpRejectionReason reason,
            int expectedStatus,
            String expectedCode) throws Exception {
        OtpActionResult rejected = new OtpActionResult(
                view(reason == OtpRejectionReason.ATTEMPT_LIMIT ? OtpStatus.FAILED : OtpStatus.PENDING, 1, 0),
                List.of(new OtpRejected(
                        UUID.randomUUID(),
                        new AggregateId(VERIFICATION_ID),
                        new UserId(USER_ID),
                        OtpPurpose.SIGN_IN,
                        reason,
                        Instant.parse("2026-07-05T10:01:00Z"))));
        when(verifyOtp.verify(any())).thenReturn(rejected);

        assertVerifyProblem("482913", expectedStatus, expectedCode);
    }

    @Test
    void hidesUnexpectedFailureDetails() throws Exception {
        when(generateOtp.generate(any())).thenThrow(new IllegalStateException("sensitive internal detail"));

        mockMvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("internal-error"))
                .andExpect(jsonPath("$.detail").value("The request could not be completed."));
    }

    private void assertVerifyProblem(String code, int expectedStatus, String expectedCode) throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyBody(code)))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    private static Stream<Arguments> applicationFailures() {
        return Stream.of(
                Arguments.of(OtpFailureReason.USER_NOT_FOUND, 404, "user-not-found"),
                Arguments.of(OtpFailureReason.OTP_NOT_FOUND, 404, "otp-not-found"),
                Arguments.of(OtpFailureReason.ACTIVE_OTP_EXISTS, 409, "active-otp-exists"),
                Arguments.of(OtpFailureReason.RESEND_LIMIT_REACHED, 429, "otp-resend-limit-exceeded"));
    }

    private static Stream<Arguments> rejections() {
        return Stream.of(
                Arguments.of(OtpRejectionReason.INVALID_CODE, 422, "otp-invalid"),
                Arguments.of(OtpRejectionReason.ATTEMPT_LIMIT, 429, "otp-verification-limit-exceeded"));
    }

    private static OtpActionResult result(OtpStatus status, int attempts, int resendCount) {
        return new OtpActionResult(view(status, attempts, resendCount), List.of());
    }

    private static OtpChallengeView view(OtpStatus status, int attempts, int resendCount) {
        return new OtpChallengeView(
                new AggregateId(VERIFICATION_ID),
                new UserId(USER_ID),
                OtpPurpose.SIGN_IN,
                status,
                EXPIRES_AT,
                attempts,
                resendCount);
    }

    private static String requestBody() {
        return """
                {"userId":"123e4567-e89b-12d3-a456-426614174000","purpose":"SIGN_IN"}
                """;
    }

    private static String verifyBody(String code) {
        return """
                {"userId":"123e4567-e89b-12d3-a456-426614174000","purpose":"SIGN_IN","code":"%s"}
                """.formatted(code);
    }
}
