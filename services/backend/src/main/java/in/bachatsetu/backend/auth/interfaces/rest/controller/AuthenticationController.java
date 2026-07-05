package in.bachatsetu.backend.auth.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.query.OtpActionResult;
import in.bachatsetu.backend.auth.application.usecase.GenerateOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.InvalidateOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.ResendOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.VerifyOtpUseCase;
import in.bachatsetu.backend.auth.interfaces.rest.dto.OtpInvalidateRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.OtpRequestRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.OtpRequestResponse;
import in.bachatsetu.backend.auth.interfaces.rest.dto.OtpResendRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.OtpVerifyRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.OtpVerifyResponse;
import in.bachatsetu.backend.auth.interfaces.rest.mapper.OtpApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes OTP use cases without leaking domain or persistence models. */
@RestController
@RequestMapping(path = "/api/v1/auth/otp", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "OTP Authentication", description = "Request, verify, resend, and invalidate OTP challenges")
@ConditionalOnProperty(
        prefix = "bachatsetu.authentication.rest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuthenticationController {

    private static final String PROBLEM_EXAMPLE = """
            {"type":"urn:bachatsetu:problem:validation-error","title":"Request validation failed",\
            "status":400,"detail":"One or more request fields are invalid.","code":"validation-error"}
            """;

    private final GenerateOtpUseCase generateOtp;
    private final VerifyOtpUseCase verifyOtp;
    private final ResendOtpUseCase resendOtp;
    private final InvalidateOtpUseCase invalidateOtp;
    private final OtpApiMapper mapper;

    public AuthenticationController(
            GenerateOtpUseCase generateOtp,
            VerifyOtpUseCase verifyOtp,
            ResendOtpUseCase resendOtp,
            InvalidateOtpUseCase invalidateOtp,
            OtpApiMapper mapper) {
        this.generateOtp = generateOtp;
        this.verifyOtp = verifyOtp;
        this.resendOtp = resendOtp;
        this.invalidateOtp = invalidateOtp;
        this.mapper = mapper;
    }

    @PostMapping(path = "/request", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Request an OTP", description = "Creates and dispatches one OTP challenge for a user and purpose.")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "OTP accepted for delivery"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = PROBLEM_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "An active OTP already exists", content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<OtpRequestResponse> request(@Valid @RequestBody OtpRequestRequest request) {
        OtpActionResult result = generateOtp.generate(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toRequestResponse(result));
    }

    @PostMapping(path = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Verify an OTP", description = "Verifies a six-digit code against the active OTP challenge.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP verified"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = PROBLEM_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "User or OTP challenge not found", content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "410", description = "OTP expired", content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "OTP invalid", content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Verification attempts exceeded", content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OtpVerifyResponse verify(@Valid @RequestBody OtpVerifyRequest request) {
        return mapper.toVerifyResponse(verifyOtp.verify(mapper.toCommand(request)));
    }

    @PostMapping(path = "/resend", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Resend an OTP", description = "Invalidates the active code and dispatches a replacement challenge.")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Replacement OTP accepted for delivery"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = PROBLEM_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "User or OTP challenge not found", content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Resend limit exceeded", content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<OtpRequestResponse> resend(@Valid @RequestBody OtpResendRequest request) {
        OtpActionResult result = resendOtp.resend(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toRequestResponse(result));
    }

    @PostMapping(path = "/invalidate", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Invalidate an OTP", description = "Immediately invalidates the active OTP challenge.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP invalidated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = PROBLEM_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "User or OTP challenge not found", content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OtpRequestResponse invalidate(@Valid @RequestBody OtpInvalidateRequest request) {
        return mapper.toRequestResponse(invalidateOtp.invalidate(mapper.toCommand(request)));
    }
}
