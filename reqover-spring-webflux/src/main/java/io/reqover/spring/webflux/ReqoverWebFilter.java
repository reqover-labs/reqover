package io.reqover.spring.webflux;

import io.reqover.core.CoverageBucket;
import io.reqover.core.CoverageContext;
import io.reqover.core.CoverageStore;
import io.reqover.core.RequestIdGenerator;
import io.reqover.core.UnitInfo;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * Binds a {@link CoverageBucket} to each WebFlux request through the Reactor
 * {@code Context} and flushes it to the store when the request terminates,
 * regardless of which scheduler thread completes it.
 */
public final class ReqoverWebFilter implements WebFilter {
    private final CoverageStore coverageStore;
    private final RequestIdGenerator requestIdGenerator;
    private final List<String> excludePathPrefixes;

    public ReqoverWebFilter(CoverageStore coverageStore, RequestIdGenerator requestIdGenerator) {
        this(coverageStore, requestIdGenerator, List.of("/reqover"));
    }

    public ReqoverWebFilter(
            CoverageStore coverageStore,
            RequestIdGenerator requestIdGenerator,
            List<String> excludePathPrefixes
    ) {
        this.coverageStore = Objects.requireNonNull(coverageStore, "coverageStore");
        this.requestIdGenerator = Objects.requireNonNull(requestIdGenerator, "requestIdGenerator");
        this.excludePathPrefixes = List.copyOf(Objects.requireNonNull(excludePathPrefixes, "excludePathPrefixes"));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (isExcluded(path)) {
            return chain.filter(exchange);
        }

        return Mono.defer(() -> {
            CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest(
                    requestIdGenerator.nextId(),
                    exchange.getRequest().getMethod().name(),
                    endpointPattern(exchange)
            ));

            return chain.filter(exchange)
                    .contextWrite(context -> context.put(ReqoverThreadLocalAccessor.KEY, bucket))
                    .doFinally(signalType -> {
                        try (CoverageContext.Scope ignored = CoverageContext.open(bucket)) {
                            bucket.updateUnitInfo(UnitInfo.httpRequest(
                                    bucket.unitInfo().unitId(),
                                    exchange.getRequest().getMethod().name(),
                                    endpointPattern(exchange)
                            ));
                            bucket.finish(statusCode(exchange));
                            coverageStore.flush(bucket);
                        }
                    });
        });
    }

    /**
     * A prefix excludes the path itself and everything below it, so
     * {@code /reqover} covers {@code /reqover/report} without also covering an
     * unrelated {@code /reqover-admin}.
     */
    private boolean isExcluded(String path) {
        for (String prefix : excludePathPrefixes) {
            if (path.equals(prefix) || path.startsWith(prefix.endsWith("/") ? prefix : prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    private static String endpointPattern(ServerWebExchange exchange) {
        Object pattern = exchange.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null && !pattern.toString().isBlank()) {
            return pattern.toString();
        }
        return exchange.getRequest().getPath().pathWithinApplication().value();
    }

    private static int statusCode(ServerWebExchange exchange) {
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        return status == null ? -1 : status.value();
    }
}
