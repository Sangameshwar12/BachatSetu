package in.bachatsetu.backend.auth.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.auth.application.signup.command.CompleteSignupCommand;
import in.bachatsetu.backend.auth.application.signup.command.StartSignupCommand;
import in.bachatsetu.backend.auth.application.signup.query.SignupCompletedResult;
import in.bachatsetu.backend.auth.application.signup.query.SignupStartedResult;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenValue;
import in.bachatsetu.backend.auth.application.token.port.RefreshTokenCredential;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.interfaces.rest.dto.SignupStartRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.SignupStartResponse;
import in.bachatsetu.backend.auth.interfaces.rest.dto.SignupVerifyRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.SignupVerifyResponse;
import in.bachatsetu.backend.shared.domain.Email;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SignupApiMapperTest {

    private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final Instant EXPIRES_AT = Instant.parse("2026-07-09T10:05:00Z");

    private final SignupApiMapper mapper = new SignupApiMapper();

    @Test
    void mapsStartRequestToCommandWithEmail() {
        SignupStartRequest request = new SignupStartRequest(
                "Asha", "Rao", "+919876543210", "asha@example.com", "ENGLISH", true);

        StartSignupCommand command = mapper.toCommand(request);

        assertThat(command.givenName()).isEqualTo("Asha");
        assertThat(command.familyName()).isEqualTo("Rao");
        assertThat(command.mobileNumber().value()).isEqualTo("+919876543210");
        assertThat(command.email()).isEqualTo(new Email("asha@example.com"));
        assertThat(command.preferredLanguage()).isEqualTo("ENGLISH");
        assertThat(command.acceptedTerms()).isTrue();
    }

    @Test
    void mapsStartRequestToCommandWithoutAnEmail() {
        SignupStartRequest request =
                new SignupStartRequest("Asha", null, "+919876543210", "", "HINDI", true);

        StartSignupCommand command = mapper.toCommand(request);

        assertThat(command.email()).isNull();
        assertThat(command.preferredLanguage()).isEqualTo("HINDI");
    }

    @Test
    void mapsSignupStartedResultToResponse() {
        SignupStartedResult result = new SignupStartedResult(new UserId(USER_ID), "+919876543210", EXPIRES_AT);

        SignupStartResponse response = mapper.toResponse(result);

        assertThat(response.userId()).isEqualTo(USER_ID.toString());
        assertThat(response.mobileNumber()).isEqualTo("+919876543210");
        assertThat(response.otpExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void mapsVerifyRequestToCommand() {
        SignupVerifyRequest request = new SignupVerifyRequest(USER_ID.toString(), "482913");

        CompleteSignupCommand command = mapper.toCommand(request);

        assertThat(command.userId()).isEqualTo(new UserId(USER_ID));
        assertThat(command.code().value()).isEqualTo("482913");
    }

    @Test
    void mapsSignupCompletedResultToResponseWithBearerTokenType() {
        RefreshTokenCredential refreshToken = RefreshTokenCredential.create(RefreshTokenId.newId(), "s".repeat(32));
        SignupCompletedResult result = new SignupCompletedResult(
                new UserId(USER_ID), AccessTokenValue.of("access-token-value"), EXPIRES_AT, refreshToken,
                EXPIRES_AT.plusSeconds(2_592_000));

        SignupVerifyResponse response = mapper.toResponse(result);

        assertThat(response.userId()).isEqualTo(USER_ID.toString());
        assertThat(response.accessToken()).isEqualTo("access-token-value");
        assertThat(response.refreshToken()).isEqualTo(refreshToken.value());
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }
}
