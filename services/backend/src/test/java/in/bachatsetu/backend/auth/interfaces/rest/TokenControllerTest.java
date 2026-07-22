package in.bachatsetu.backend.auth.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.token.exception.TokenApplicationException;
import in.bachatsetu.backend.auth.application.token.exception.TokenFailureReason;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenClaims;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenValue;
import in.bachatsetu.backend.auth.application.token.port.IssuedAccessToken;
import in.bachatsetu.backend.auth.application.token.port.RefreshTokenCredential;
import in.bachatsetu.backend.auth.application.token.query.RefreshTokenResult;
import in.bachatsetu.backend.auth.application.token.query.RefreshTokenState;
import in.bachatsetu.backend.auth.application.token.query.TokenPairResult;
import in.bachatsetu.backend.auth.application.token.usecase.RefreshAccessTokenUseCase;
import in.bachatsetu.backend.auth.application.token.usecase.RevokeRefreshTokenUseCase;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.TokenSessionId;
import in.bachatsetu.backend.auth.domain.model.TokenStatus;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.interfaces.rest.config.AuthenticationOpenApiConfig;
import in.bachatsetu.backend.auth.interfaces.rest.controller.TokenController;
import in.bachatsetu.backend.auth.interfaces.rest.exception.GlobalExceptionHandler;
import in.bachatsetu.backend.auth.interfaces.rest.mapper.TokenApiMapper;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Set;
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

@WebMvcTest(TokenController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({TokenApiMapper.class, GlobalExceptionHandler.class, AuthenticationOpenApiConfig.class})
class TokenControllerTest {

    private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final Instant NOW = Instant.parse("2026-07-12T10:00:00Z");
    private static final String VALID_REFRESH_TOKEN = RefreshTokenId.newId() + "." + "s".repeat(32);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RefreshAccessTokenUseCase refreshAccessToken;

    @MockBean
    private RevokeRefreshTokenUseCase revokeRefreshToken;

    @Test
    void rotatesARefreshTokenAndReturnsANewPair() throws Exception {
        when(refreshAccessToken.refresh(any())).thenReturn(tokenPairResult());

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(VALID_REFRESH_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.accessToken").value("access-token-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void rejectsRefreshWithAMalformedToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody("not-a-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid-refresh-token"));
    }

    @ParameterizedTest
    @MethodSource("tokenFailures")
    void mapsRefreshFailuresToProblemDetails(
            TokenFailureReason reason, int expectedStatus, String expectedCode) throws Exception {
        when(refreshAccessToken.refresh(any())).thenThrow(new TokenApplicationException(reason, "safe failure"));

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(VALID_REFRESH_TOKEN)))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    @Test
    void logsOutAndReturnsNoContent() throws Exception {
        when(revokeRefreshToken.revoke(any())).thenReturn(
                new RefreshTokenState(RefreshTokenId.newId(), TokenStatus.REVOKED));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(VALID_REFRESH_TOKEN)))
                .andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @MethodSource("tokenFailures")
    void mapsLogoutFailuresToProblemDetails(
            TokenFailureReason reason, int expectedStatus, String expectedCode) throws Exception {
        when(revokeRefreshToken.revoke(any())).thenThrow(new TokenApplicationException(reason, "safe failure"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(VALID_REFRESH_TOKEN)))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    private static Stream<Arguments> tokenFailures() {
        return Stream.of(
                Arguments.of(TokenFailureReason.REFRESH_TOKEN_EXPIRED, 401, "refresh-token-expired"),
                Arguments.of(TokenFailureReason.REFRESH_TOKEN_REVOKED, 401, "refresh-token-revoked"),
                Arguments.of(TokenFailureReason.REFRESH_TOKEN_REUSED, 401, "refresh-token-reused"),
                Arguments.of(TokenFailureReason.INVALID_REFRESH_TOKEN, 401, "invalid-refresh-token"),
                Arguments.of(TokenFailureReason.REFRESH_TOKEN_NOT_FOUND, 401, "invalid-refresh-token"),
                Arguments.of(TokenFailureReason.USER_NOT_FOUND, 404, "user-not-found"),
                Arguments.of(TokenFailureReason.USER_NOT_ACTIVE, 403, "user-not-active"),
                Arguments.of(TokenFailureReason.ROLE_NOT_FOUND, 500, "internal-error"),
                Arguments.of(TokenFailureReason.PERMISSION_NOT_FOUND, 500, "internal-error"),
                Arguments.of(TokenFailureReason.ACTIVE_REFRESH_TOKEN_EXISTS, 409, "active-refresh-token-exists"),
                Arguments.of(TokenFailureReason.REFRESH_TOKEN_CONFLICT, 401, "refresh-token-conflict"));
    }

    private TokenPairResult tokenPairResult() {
        AccessTokenClaims claims = new AccessTokenClaims(
                new UserId(USER_ID), MobileNumber.of("+919876543210"), AggregateId.newId(),
                Set.of("GROUP_MEMBER"), Set.of(), NOW, NOW.plusSeconds(900), "bachatsetu", "bachatsetu-api", 1);
        IssuedAccessToken accessToken = new IssuedAccessToken(AccessTokenValue.of("access-token-value"), claims);
        RefreshTokenCredential credential = RefreshTokenCredential.create(RefreshTokenId.newId(), "s".repeat(32));
        RefreshTokenResult refreshToken =
                new RefreshTokenResult(credential, TokenSessionId.newId(), NOW.plusSeconds(2_592_000));
        return new TokenPairResult(accessToken, refreshToken);
    }

    private static String refreshBody(String refreshToken) {
        return """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);
    }
}
