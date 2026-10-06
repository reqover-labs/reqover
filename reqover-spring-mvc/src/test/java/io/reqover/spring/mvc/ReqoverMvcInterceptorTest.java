package io.reqover.spring.mvc;

import io.reqover.core.CoverageBucketSnapshot;
import io.reqover.core.InMemoryCoverageStore;
import io.reqover.core.RequestIdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReqoverMvcInterceptorTest {
    private final InMemoryCoverageStore store = new InMemoryCoverageStore();
    private final ReqoverMvcInterceptor interceptor = new ReqoverMvcInterceptor(store, new RequestIdGenerator());

    @Test
    void skipsTheCatchAllResourceHandler() {
        handle("/no-such-endpoint", "/**", new ResourceHttpRequestHandler(), 404);

        assertTrue(store.snapshots().isEmpty(), store.snapshots().toString());
    }

    @Test
    void recordsAResourceHandlerMappedToItsOwnPattern() {
        handle("/downloads/report.csv", "/downloads/**", new ResourceHttpRequestHandler(), 200);

        List<CoverageBucketSnapshot> snapshots = store.snapshots();
        assertEquals(1, snapshots.size());
        assertEquals("GET /downloads/**", snapshots.get(0).unitInfo().name());
    }

    private void handle(String uri, String pattern, Object handler, int status) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, pattern);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(status);

        interceptor.preHandle(request, response, handler);
        interceptor.afterCompletion(request, response, handler, null);
    }
}
