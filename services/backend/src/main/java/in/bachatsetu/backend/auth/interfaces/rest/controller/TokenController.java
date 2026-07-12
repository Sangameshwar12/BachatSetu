package in.bachatsetu.backend.auth.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.token.query.TokenPairResult;
import in.bachatsetu.backend.auth.application.token.usecase.RefreshAccessTokenUseCase;
import in.bachatsetu.backend.auth.application.token.usecase.RevokeRefreshTokenUseCase;
import in.bachatsetu.backend.auth.interfaces.rest.dto.LogoutRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.RefreshTokenRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.RefreshTokenResponse;
import in.bachatsetu.backend.auth.interfaces.rest.mapper.TokenApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes refresh-token rotation and logout on top of Sprint 8.6's already-built token
 * application services — no new token generation, rotation, or revocation logic, only the REST
 * boundary that was never wired to it.
 */
@RestController
@RequestMapping(path = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Session", description = "Refresh-token rotation and logout")
@ConditionalOnProperty(
        name = {"bachatsetu.authentication.rest.enabled", "bachatsetu.authentication.token.enabled"},
        havingValue = "true",
        matchIfMissing = true)
public class TokenController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final RefreshAccessTokenUseCase refreshAccessToken;
    private final RevokeRefreshTokenUseCase revokeRefreshToken;
    private final TokenApiMapper mapper;

    public TokenController(
            RefreshAccessTokenUseCase refreshAccessToken,
            RevokeRefreshTokenUseCase revokeRefreshToken,
            TokenApiMapper mapper) {
        this.refreshAccessToken = refreshAccessToken;
        this.revokeRefreshToken = revokeRefreshToken;
        this.mapper = mapper;
    }

    @PostMapping(path = "/token/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Rotate a refresh token",
            description = "Validates, rotates, and replaces a refresh token, issuing a fresh access token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New access and refresh token pair issued"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Refresh token invalid, expired, revoked, or reused", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public RefreshTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenPairResult result = refreshAccessToken.refresh(mapper.toCommand(request));
        return mapper.toResponse(result);
    }

    @PostMapping(path = "/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Log out",
            description = "Revokes a refresh token, ending its session.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Refresh token revoked"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Refresh token invalid, expired, revoked, or reused", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        revokeRefreshToken.revoke(mapper.toCommand(request));
        return ResponseEntity.noContent().build();
    }
}
