package in.bachatsetu.backend.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenClaims;
import in.bachatsetu.backend.auth.application.token.port.JwtValidationException;
import in.bachatsetu.backend.auth.application.token.port.JwtValidationFailure;
import in.bachatsetu.backend.auth.application.token.usecase.ValidateAccessTokenUseCase;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.security.config.AuthenticationSecurityProperties;
import in.bachatsetu.backend.shared.domain.AggregateId;
import jakarta.servlet.FilterChain;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void skipsRequestsWithoutHeaderAndPreservesExistingAuthentication() throws Exception {
        ValidateAccessTokenUseCase validator = mock(ValidateAccessTokenUseCase.class);
        AuthenticationEntryPoint entryPoint = mock(AuthenticationEntryPoint.class);
        JwtAuthenticationFilter filter = filter(() -> validator, entryPoint);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(validator, never()).validate(any());

        Authentication existing = UsernamePasswordAuthenticationToken.authenticated(
                "existing", null, AuthorityUtils.NO_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(existing);
        request.addHeader("Authorization", "Bearer compact.jwt.value");
        filter.doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verify(validator, never()).validate(any());
    }

    @Test
    void validatesCaseInsensitiveBearerPrefixAndPopulatesContext() throws Exception {
        ValidateAccessTokenUseCase validator = mock(ValidateAccessTokenUseCase.class);
        when(validator.validate(any())).thenReturn(claims());
        AuthenticationEntryPoint entryPoint = mock(AuthenticationEntryPoint.class);
        JwtAuthenticationFilter filter = filter(() -> validator, entryPoint);
        FilterChain chain = mock(FilterChain.class);
        AtomicReference<Authentication> authentication = new AtomicReference<>();
        doAnswer(invocation -> {
            authentication.set(SecurityContextHolder.getContext().getAuthentication());
            return null;
        }).when(chain).doFilter(any(), any());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "bearer compact.jwt.value");

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(authentication.get().isAuthenticated()).isTrue();
        assertThat(authentication.get().getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        assertThat(authentication.get().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_GROUP_MEMBER", "group.read");
        verify(entryPoint, never()).commence(any(), any(), any());
    }

    @Test
    void rejectsMalformedHeadersEmptyTokensAndUnavailableValidator() throws Exception {
        AuthenticationEntryPoint entryPoint = mock(AuthenticationEntryPoint.class);
        FilterChain chain = mock(FilterChain.class);

        assertRejected(filter(() -> mock(ValidateAccessTokenUseCase.class), entryPoint),
                "Basic credential", entryPoint, chain);
        assertRejected(filter(() -> mock(ValidateAccessTokenUseCase.class), entryPoint),
                "Bearer   ", entryPoint, chain);
        assertRejected(filter(() -> null, entryPoint),
                "Bearer compact.jwt.value", entryPoint, chain);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void rejectsValidationAndClaimTranslationFailures() throws Exception {
        AuthenticationEntryPoint entryPoint = mock(AuthenticationEntryPoint.class);
        FilterChain chain = mock(FilterChain.class);
        ValidateAccessTokenUseCase invalidJwt = mock(ValidateAccessTokenUseCase.class);
        when(invalidJwt.validate(any())).thenThrow(new JwtValidationException(
                JwtValidationFailure.INVALID_SIGNATURE, "invalid"));
        assertRejected(filter(() -> invalidJwt, entryPoint),
                "Bearer compact.jwt.value", entryPoint, chain);

        ValidateAccessTokenUseCase invalidClaims = mock(ValidateAccessTokenUseCase.class);
        when(invalidClaims.validate(any())).thenThrow(new IllegalArgumentException("claims"));
        assertRejected(filter(() -> invalidClaims, entryPoint),
                "Bearer compact.jwt.value", entryPoint, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void assertRejected(
            JwtAuthenticationFilter filter,
            String header,
            AuthenticationEntryPoint entryPoint,
            FilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", header);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(entryPoint).commence(any(), any(), any());
        clearInvocations(entryPoint);
        SecurityContextHolder.clearContext();
    }

    private JwtAuthenticationFilter filter(
            java.util.function.Supplier<ValidateAccessTokenUseCase> validator,
            AuthenticationEntryPoint entryPoint) {
        return new JwtAuthenticationFilter(validator, properties(), entryPoint);
    }

    private AccessTokenClaims claims() {
        Instant issuedAt = Instant.parse("2026-07-06T00:00:00Z");
        return new AccessTokenClaims(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                Set.of("GROUP_MEMBER"),
                Set.of("group.read"),
                issuedAt,
                issuedAt.plusSeconds(900),
                "bachatsetu",
                "bachatsetu-api",
                1);
    }

    private AuthenticationSecurityProperties properties() {
        return new AuthenticationSecurityProperties(
                "Authorization",
                "Bearer ",
                Duration.ofSeconds(30),
                12,
                List.of("/api/v1/auth/**"),
                new AuthenticationSecurityProperties.Cors(
                        List.of("https://app.example.com"),
                        List.of("GET", "POST"),
                        List.of("Authorization"),
                        List.of("X-Request-ID"),
                        false,
                        Duration.ofHours(1)));
    }
}
