package in.bachatsetu.backend.security.filter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bachatsetu.backend.auth.application.port.RateLimiterPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LoginRateLimitFilterTest {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Test
    void allowsARequestUnderTheLimit() throws Exception {
        RateLimiterPort rateLimiter = mock(RateLimiterPort.class);
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(true);
        LoginRateLimitFilter filter = new LoginRateLimitFilter(() -> rateLimiter, 20, WINDOW, new ObjectMapper());
        HttpServletRequest request = mockRequest("POST", "/api/v1/auth/login/start", "203.0.113.5");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsWithTooManyRequestsOnceTheLimitIsExceeded() throws Exception {
        RateLimiterPort rateLimiter = mock(RateLimiterPort.class);
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(false);
        LoginRateLimitFilter filter = new LoginRateLimitFilter(() -> rateLimiter, 20, WINDOW, new ObjectMapper());
        HttpServletRequest request = mockRequest("POST", "/api/v1/auth/otp/request", "203.0.113.5");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(429);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void keysTheLimitByClientIpAddress() throws Exception {
        RateLimiterPort rateLimiter = mock(RateLimiterPort.class);
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(true);
        LoginRateLimitFilter filter = new LoginRateLimitFilter(() -> rateLimiter, 20, WINDOW, new ObjectMapper());
        HttpServletRequest request = mockRequest("POST", "/api/v1/auth/signup", "198.51.100.9");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, mock(HttpServletResponse.class), chain);

        verify(rateLimiter).tryConsume("login-ip:198.51.100.9", 20, WINDOW);
    }

    @Test
    void leavesUnrelatedPathsUngated() throws Exception {
        RateLimiterPort rateLimiter = mock(RateLimiterPort.class);
        LoginRateLimitFilter filter = new LoginRateLimitFilter(() -> rateLimiter, 20, WINDOW, new ObjectMapper());
        HttpServletRequest request = mockRequest("POST", "/api/v1/auth/login/verify", "203.0.113.5");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(rateLimiter, never()).tryConsume(any(), anyInt(), any());
    }

    @Test
    void leavesNonPostRequestsToARateLimitedPathUngated() throws Exception {
        RateLimiterPort rateLimiter = mock(RateLimiterPort.class);
        LoginRateLimitFilter filter = new LoginRateLimitFilter(() -> rateLimiter, 20, WINDOW, new ObjectMapper());
        HttpServletRequest request = mockRequest("GET", "/api/v1/auth/login/start", "203.0.113.5");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(rateLimiter, never()).tryConsume(any(), anyInt(), any());
    }

    @Test
    void failsOpenWhenTheRateLimiterIsUnavailable() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter(() -> null, 20, WINDOW, new ObjectMapper());
        HttpServletRequest request = mockRequest("POST", "/api/v1/auth/login/start", "203.0.113.5");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsNonPositiveMaxAttempts() {
        RateLimiterPort rateLimiter = mock(RateLimiterPort.class);
        assertThatThrownBy(() -> new LoginRateLimitFilter(() -> rateLimiter, 0, WINDOW, new ObjectMapper()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private HttpServletRequest mockRequest(String method, String path, String remoteAddress) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getRemoteAddr()).thenReturn(remoteAddress);
        return request;
    }
}
