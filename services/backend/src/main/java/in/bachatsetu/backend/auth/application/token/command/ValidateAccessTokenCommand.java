package in.bachatsetu.backend.auth.application.token.command;

import in.bachatsetu.backend.auth.application.token.port.AccessTokenValue;
import java.util.Objects;

public record ValidateAccessTokenCommand(AccessTokenValue token) {

    public ValidateAccessTokenCommand {
        Objects.requireNonNull(token, "access token must not be null");
    }
}
