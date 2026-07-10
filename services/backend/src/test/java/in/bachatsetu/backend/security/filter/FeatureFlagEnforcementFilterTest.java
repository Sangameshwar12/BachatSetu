package in.bachatsetu.backend.security.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bachatsetu.backend.admin.application.configuration.service.FeatureFlagQueryService;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import org.junit.jupiter.api.Test;

class FeatureFlagEnforcementFilterTest {

    @Test
    void allowsTheRequestWhenTheMappedFeatureIsEnabled() throws Exception {
        FeatureFlagQueryService featureFlagService = mock(FeatureFlagQueryService.class);
        when(featureFlagService.isEnabled(FeatureKey.PAYMENTS)).thenReturn(true);
        FeatureFlagEnforcementFilter filter =
                new FeatureFlagEnforcementFilter(() -> featureFlagService, new ObjectMapper());
        HttpServletRequest request = mockRequest("/api/v1/payments/123");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsWithServiceUnavailableWhenTheMappedFeatureIsDisabled() throws Exception {
        FeatureFlagQueryService featureFlagService = mock(FeatureFlagQueryService.class);
        when(featureFlagService.isEnabled(FeatureKey.NOTIFICATIONS)).thenReturn(false);
        FeatureFlagEnforcementFilter filter =
                new FeatureFlagEnforcementFilter(() -> featureFlagService, new ObjectMapper());
        HttpServletRequest request = mockRequest("/api/v1/notifications");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(503);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void neverGatesAdminEndpointsExceptAnalytics() throws Exception {
        FeatureFlagQueryService featureFlagService = mock(FeatureFlagQueryService.class);
        FeatureFlagEnforcementFilter filter =
                new FeatureFlagEnforcementFilter(() -> featureFlagService, new ObjectMapper());
        HttpServletRequest request = mockRequest("/api/v1/admin/config");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void gatesAdminAnalyticsByTheAnalyticsFlag() throws Exception {
        FeatureFlagQueryService featureFlagService = mock(FeatureFlagQueryService.class);
        when(featureFlagService.isEnabled(FeatureKey.ANALYTICS)).thenReturn(false);
        FeatureFlagEnforcementFilter filter =
                new FeatureFlagEnforcementFilter(() -> featureFlagService, new ObjectMapper());
        HttpServletRequest request = mockRequest("/api/v1/admin/analytics/overview");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(503);
    }

    @Test
    void failsOpenWhenTheFeatureFlagServiceIsUnavailable() throws Exception {
        FeatureFlagEnforcementFilter filter = new FeatureFlagEnforcementFilter(() -> null, new ObjectMapper());
        HttpServletRequest request = mockRequest("/api/v1/storage/files/123");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void gatesReceiptsAuctionsAndAuditByTheirOwnFlags() throws Exception {
        FeatureFlagQueryService featureFlagService = mock(FeatureFlagQueryService.class);
        when(featureFlagService.isEnabled(FeatureKey.RECEIPTS)).thenReturn(true);
        when(featureFlagService.isEnabled(FeatureKey.AUCTION)).thenReturn(true);
        when(featureFlagService.isEnabled(FeatureKey.AUDIT)).thenReturn(true);
        FeatureFlagEnforcementFilter filter =
                new FeatureFlagEnforcementFilter(() -> featureFlagService, new ObjectMapper());
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(mockRequest("/api/v1/receipts/1"), mock(HttpServletResponse.class), chain);
        filter.doFilter(mockRequest("/api/v1/auctions/1"), mock(HttpServletResponse.class), chain);
        filter.doFilter(mockRequest("/api/v1/audit"), mock(HttpServletResponse.class), chain);

        verify(featureFlagService).isEnabled(FeatureKey.RECEIPTS);
        verify(featureFlagService).isEnabled(FeatureKey.AUCTION);
        verify(featureFlagService).isEnabled(FeatureKey.AUDIT);
    }

    @Test
    void leavesUnmappedPathsUngated() throws Exception {
        FeatureFlagQueryService featureFlagService = mock(FeatureFlagQueryService.class);
        FeatureFlagEnforcementFilter filter =
                new FeatureFlagEnforcementFilter(() -> featureFlagService, new ObjectMapper());
        HttpServletRequest request = mockRequest("/actuator/health");
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
