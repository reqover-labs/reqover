package io.reqover.spring.mvc;

import io.reqover.core.CoverageBucket;
import io.reqover.core.CoverageContext;
import io.reqover.core.InMemoryCoverageStore;
import io.reqover.core.RequestIdGenerator;
import io.reqover.core.UnitInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Objects;

public final class ReqoverMvcInterceptor implements HandlerInterceptor {
    private static final String BUCKET_ATTRIBUTE = ReqoverMvcInterceptor.class.getName() + ".bucket";

    private final InMemoryCoverageStore coverageStore;
    private final RequestIdGenerator requestIdGenerator;

    public ReqoverMvcInterceptor(InMemoryCoverageStore coverageStore, RequestIdGenerator requestIdGenerator) {
        this.coverageStore = Objects.requireNonNull(coverageStore, "coverageStore");
        this.requestIdGenerator = Objects.requireNonNull(requestIdGenerator, "requestIdGenerator");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
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
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        Object value = request.getAttribute(BUCKET_ATTRIBUTE);
        try {
            if (value instanceof CoverageBucket bucket) {
                bucket.finish(response.getStatus());
                coverageStore.flush(bucket);
            }
        } finally {
            CoverageContext.clear();
            request.removeAttribute(BUCKET_ATTRIBUTE);
        }
    }

    private static String endpointPattern(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null && !pattern.toString().isBlank()) {
            return pattern.toString();
        }
        return request.getRequestURI();
    }
}

