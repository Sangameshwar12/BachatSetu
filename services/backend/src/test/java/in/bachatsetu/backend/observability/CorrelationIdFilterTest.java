package in.bachatsetu.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesARequestIdAndEchoesItAsAResponseHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/groups");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertThat(MDC.get(CorrelationIdFilter.REQUEST_ID_MDC_KEY)).isNotBlank();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER)).isNotBlank();
        assertThat(MDC.get(CorrelationIdFilter.REQUEST_ID_MDC_KEY)).isNull();
    }

    @Test
    void preservesAnUpstreamCorrelationIdInsteadOfReplacingIt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/groups");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "upstream-correlation-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY))
                .isEqualTo("upstream-correlation-id");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("upstream-correlation-id");
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    void generatesADistinctCorrelationIdWhenNoneIsSupplied() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/groups");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isNotBlank();
        assertThat(response.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER)).isNotBlank();
    }
}
