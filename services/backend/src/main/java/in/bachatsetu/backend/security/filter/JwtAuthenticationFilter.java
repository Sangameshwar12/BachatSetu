package in.bachatsetu.backend.security.filter;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.token.command.ValidateAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenClaims;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenValue;
import in.bachatsetu.backend.auth.application.token.port.JwtValidationException;
import in.bachatsetu.backend.auth.application.token.usecase.ValidateAccessTokenUseCase;
import in.bachatsetu.backend.security.config.AuthenticationSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

/** Validates bearer JWTs and establishes the request SecurityContext. */
public final class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROLE_PREFIX = "ROLE_";

    private final Supplier<ValidateAccessTokenUseCase> validatorSupplier;
    private final AuthenticationSecurityProperties properties;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            Supplier<ValidateAccessTokenUseCase> validatorSupplier,
            AuthenticationSecurityProperties properties,
            AuthenticationEntryPoint authenticationEntryPoint) {
        this.validatorSupplier = Objects.requireNonNull(validatorSupplier, "token validator supplier must not be null");
        this.properties = Objects.requireNonNull(properties, "security properties must not be null");
        this.authenticationEntryPoint = Objects.requireNonNull(
                authenticationEntryPoint, "authentication entry point must not be null");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(properties.headerName());
        if (header == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!hasConfiguredPrefix(header)) {
            reject(request, response, null);
            return;
        }
        String compactToken = header.substring(properties.bearerPrefix().length()).strip();
        if (compactToken.isEmpty()) {
            reject(request, response, null);
            return;
        }
        try {
            ValidateAccessTokenUseCase validator = validatorSupplier.get();
            if (validator == null) {
                reject(request, response, null);
                return;
            }
            AccessTokenClaims claims = validator.validate(
                    new ValidateAccessTokenCommand(AccessTokenValue.of(compactToken)));
            AuthenticatedUser principal = new AuthenticatedUser(
                    claims.userId(),
                    claims.mobileNumber(),
                    claims.tenantId(),
                    claims.roles(),
                    claims.permissions());
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    principal, null, authorities(principal));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            filterChain.doFilter(request, response);
        } catch (JwtValidationException | IllegalArgumentException exception) {
            reject(request, response, exception);
        }
    }

    private boolean hasConfiguredPrefix(String header) {
        String prefix = properties.bearerPrefix();
        return header.length() >= prefix.length()
                && header.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private Collection<? extends GrantedAuthority> authorities(AuthenticatedUser principal) {
        return Stream.concat(
                        principal.roles().stream().map(role -> ROLE_PREFIX + role),
                        principal.permissions().stream())
                .sorted()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private void reject(
            HttpServletRequest request,
            HttpServletResponse response,
            Exception cause) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        BadCredentialsException failure = cause == null
                ? new BadCredentialsException("Invalid bearer access token")
                : new BadCredentialsException("Invalid bearer access token", cause);
        authenticationEntryPoint.commence(request, response, failure);
    }
}
