package io.reqover.spring.mvc;

import io.reqover.core.CoverageBucket;
import io.reqover.core.CoverageContext;
import io.reqover.core.CoverageStore;
import io.reqover.core.RequestIdGenerator;
import io.reqover.core.UnitInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import java.util.Objects;

/**
 * Binds a {@link CoverageBucket} to each MVC request and flushes it to the
 * store when the request completes.
 *
 * <p>For Servlet async processing, the bucket created on the initial dispatch
 * is reused on the async re-dispatch. The container thread's coverage context
 * is cleared as soon as concurrent handling starts so unrelated work on that
 * thread cannot record into a foreign bucket. Application code executed on an
 * async worker thread is not attributed until the request is re-dispatched.
 *
 * <p>Requests served by Spring Boot's catch-all static resource handler are not
 * recorded. Boot maps it to {@code /**}, so every URL that matches no controller
 * lands there and would otherwise pile up under a single {@code GET /**}
 * endpoint. A resource handler mapped to any other pattern is recorded.
 */
public final class ReqoverMvcInterceptor implements AsyncHandlerInterceptor {
    private static final String BUCKET_ATTRIBUTE = ReqoverMvcInterceptor.class.getName() + ".bucket";
    private static final String CATCH_ALL_PATTERN = "/**";

    private final CoverageStore coverageStore;
    private final RequestIdGenerator requestIdGenerator;

    public ReqoverMvcInterceptor(CoverageStore coverageStore, RequestIdGenerator requestIdGenerator) {
        this.coverageStore = Objects.requireNonNull(coverageStore, "coverageStore");
        this.requestIdGenerator = Objects.requireNonNull(requestIdGenerator, "requestIdGenerator");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getAttribute(BUCKET_ATTRIBUTE) instanceof CoverageBucket existing) {
            CoverageContext.set(existing);
            return true;
        }
        if (isCatchAllResourceHandler(request, handler)) {
            return true;
        }

        CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest(
                requestIdGenerator.nextId(),
                request.getMethod(),
                endpointPattern(request)
        ));
        request.setAttribute(BUCKET_ATTRIBUTE, bucket);
        CoverageContext.set(bucket);
        return true;
    }

    @Override
    public void afterConcurrentHandlingStarted(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        CoverageContext.clear();
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        Object value = request.getAttribute(BUCKET_ATTRIBUTE);
        try {
            if (value instanceof CoverageBucket bucket) {
                bucket.finish(statusCode(response, ex));
                coverageStore.flush(bucket);
            }
        } finally {
            CoverageContext.clear();
            request.removeAttribute(BUCKET_ATTRIBUTE);
        }
    }

    private static int statusCode(HttpServletResponse response, Exception ex) {
        int status = response.getStatus();
        return ex != null && status < 400 ? 500 : status;
    }

    private static boolean isCatchAllResourceHandler(HttpServletRequest request, Object handler) {
        return handler instanceof ResourceHttpRequestHandler
                && CATCH_ALL_PATTERN.equals(String.valueOf(
                        request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)));
    }

    private static String endpointPattern(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null && !pattern.toString().isBlank()) {
            return pattern.toString();
        }
        return request.getRequestURI();
    }
}
