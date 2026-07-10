package in.bachatsetu.backend.security.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bachatsetu.backend.admin.application.configuration.service.MaintenanceStatus;
import in.bachatsetu.backend.admin.application.configuration.service.MaintenanceStatusQueryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class MaintenanceModeFilterTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void allowsTheRequestWhenMaintenanceIsNotActive() throws Exception {
        MaintenanceStatusQueryService statusService = mock(MaintenanceStatusQueryService.class);
        when(statusService.currentStatus(NOW)).thenReturn(new MaintenanceStatus(false, null));
        MaintenanceModeFilter filter = new MaintenanceModeFilter(() -> statusService, CLOCK, new ObjectMapper());
        HttpServletRequest request = mockRequest("/api/v1/payments");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsWithServiceUnavailableWhenMaintenanceIsActive() throws Exception {
        SecurityContextHolder.clearContext();
        MaintenanceStatusQueryService statusService = mock(MaintenanceStatusQueryService.class);
        when(statusService.currentStatus(NOW)).thenReturn(new MaintenanceStatus(true, "down for maintenance"));
        MaintenanceModeFilter filter = new MaintenanceModeFilter(() -> statusService, CLOCK, new ObjectMapper());
        HttpServletRequest request = mockRequest("/api/v1/payments");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(503);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void alwaysAllowsAuthEndpointsRegardlessOfMaintenance() throws Exception {
        MaintenanceStatusQueryService statusService = mock(MaintenanceStatusQueryService.class);
        MaintenanceModeFilter filter = new MaintenanceModeFilter(() -> statusService, CLOCK, new ObjectMapper());
        HttpServletRequest request = mockRequest("/api/v1/auth/otp/request");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void alwaysAllowsPlatformAdministratorsRegardlessOfMaintenance() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))));
        try {
            MaintenanceStatusQueryService statusService = mock(MaintenanceStatusQueryService.class);
            MaintenanceModeFilter filter = new MaintenanceModeFilter(() -> statusService, CLOCK, new ObjectMapper());
            HttpServletRequest request = mockRequest("/api/v1/payments");
            HttpServletResponse response = mock(HttpServletResponse.class);
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void failsOpenWhenTheStatusServiceIsUnavailable() throws Exception {
        SecurityContextHolder.clearContext();
        MaintenanceModeFilter filter = new MaintenanceModeFilter(() -> null, CLOCK, new ObjectMapper());
        HttpServletRequest request = mockRequest("/api/v1/payments");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    private HttpServletRequest mockRequest(String path) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(path);
        return request;
    }
}
