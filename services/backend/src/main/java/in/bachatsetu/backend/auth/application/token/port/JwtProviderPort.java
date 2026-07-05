package in.bachatsetu.backend.auth.application.token.port;

/** Algorithm-independent JWT signing and validation boundary. */
public interface JwtProviderPort {

    IssuedAccessToken issue(AccessTokenPrincipal principal);

    AccessTokenClaims validate(AccessTokenValue token);
}
