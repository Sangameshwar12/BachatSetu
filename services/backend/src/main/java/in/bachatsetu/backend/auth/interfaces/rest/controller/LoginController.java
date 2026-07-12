package in.bachatsetu.backend.auth.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.login.query.LoginCompletedResult;
import in.bachatsetu.backend.auth.application.login.query.LoginStartedResult;
import in.bachatsetu.backend.auth.application.login.usecase.CompleteLoginUseCase;
import in.bachatsetu.backend.auth.application.login.usecase.StartLoginUseCase;
import in.bachatsetu.backend.auth.interfaces.rest.dto.LoginStartRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.LoginStartResponse;
import in.bachatsetu.backend.auth.interfaces.rest.dto.LoginVerifyRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.LoginVerifyResponse;
import in.bachatsetu.backend.auth.interfaces.rest.mapper.LoginApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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

/** Exposes the returning-user login (sign-in) flow without leaking domain or persistence models. */
@RestController
@RequestMapping(path = "/api/v1/auth/login", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Login", description = "Returning-user sign-in: verify an existing account by OTP and receive tokens")
@ConditionalOnProperty(
        name = {"bachatsetu.authentication.rest.enabled", "bachatsetu.authentication.token.enabled"},
        havingValue = "true",
        matchIfMissing = true)
public class LoginController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final StartLoginUseCase startLogin;
    private final CompleteLoginUseCase completeLogin;
    private final LoginApiMapper mapper;

    public LoginController(StartLoginUseCase startLogin, CompleteLoginUseCase completeLogin, LoginApiMapper mapper) {
        this.startLogin = startLogin;
        this.completeLogin = completeLogin;
        this.mapper = mapper;
    }

    @PostMapping(path = "/start", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Start returning-user login",
            description = "Looks up the account by mobile number and dispatches a sign-in OTP.")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "OTP dispatched"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "No account registered for this mobile number", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Active OTP already exists", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<LoginStartResponse> start(@Valid @RequestBody LoginStartRequest request) {
        LoginStartedResult result = startLogin.execute(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toResponse(result));
    }

    @PostMapping(path = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Verify sign-in OTP",
            description = "Verifies the sign-in OTP and issues an access and refresh token pair.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Signed in; tokens issued"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "No pending login for this user", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "410", description = "OTP expired", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "OTP invalid", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Verification attempts exceeded", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public LoginVerifyResponse verify(@Valid @RequestBody LoginVerifyRequest request) {
        LoginCompletedResult result = completeLogin.execute(mapper.toCommand(request));
        return mapper.toResponse(result);
    }
}
