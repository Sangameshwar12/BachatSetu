package in.bachatsetu.backend.auth.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.service.GenerateOtpApplicationService;
import in.bachatsetu.backend.auth.application.signup.query.SignupCompletedResult;
import in.bachatsetu.backend.auth.application.signup.query.SignupStartedResult;
import in.bachatsetu.backend.auth.application.signup.usecase.CompleteSignupUseCase;
import in.bachatsetu.backend.auth.application.signup.usecase.StartSignupUseCase;
import in.bachatsetu.backend.auth.interfaces.rest.dto.SignupStartRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.SignupStartResponse;
import in.bachatsetu.backend.auth.interfaces.rest.dto.SignupVerifyRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.SignupVerifyResponse;
import in.bachatsetu.backend.auth.interfaces.rest.mapper.SignupApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the self-registration (signup) flow without leaking domain or persistence models. */
@RestController
@RequestMapping(path = "/api/v1/auth/signup", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Signup", description = "Self-registration: create an account, verify it, and receive tokens")
@ConditionalOnProperty(
        name = {"bachatsetu.authentication.rest.enabled", "bachatsetu.authentication.token.enabled"},
        havingValue = "true",
        matchIfMissing = true)
public class SignupController {

        private static final Logger log =
            LoggerFactory.getLogger(GenerateOtpApplicationService.class);

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final StartSignupUseCase startSignup;
    private final CompleteSignupUseCase completeSignup;
    private final SignupApiMapper mapper;

    public SignupController(
            StartSignupUseCase startSignup, CompleteSignupUseCase completeSignup, SignupApiMapper mapper) {
        this.startSignup = startSignup;
        this.completeSignup = completeSignup;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Start self-registration",
            description = "Creates an account pending verification and dispatches a registration OTP.")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Account created; OTP dispatched"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Mobile number or email already registered", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<SignupStartResponse> start(@Valid @RequestBody SignupStartRequest request) {
        log.info("Received signup request for mobile number: ");
        SignupStartedResult result = startSignup.execute(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toResponse(result));
    }

    @PostMapping(path = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Verify signup OTP",
            description = "Verifies the registration OTP, activates the account, and issues tokens.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account activated; tokens issued"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "No pending account for this user", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "410", description = "OTP expired", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "OTP invalid", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Verification attempts exceeded", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public SignupVerifyResponse verify(@Valid @RequestBody SignupVerifyRequest request) {
        SignupCompletedResult result = completeSignup.execute(mapper.toCommand(request));
        return mapper.toResponse(result);
    }
}
