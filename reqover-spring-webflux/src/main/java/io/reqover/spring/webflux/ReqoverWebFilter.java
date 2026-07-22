package io.reqover.spring.webflux;

import io.reqover.core.CoverageBucket;
import io.reqover.core.CoverageContext;
import io.reqover.core.InMemoryCoverageStore;
import io.reqover.core.RequestIdGenerator;
import io.reqover.core.UnitInfo;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ReqoverWebFilter implements WebFilter {
    private final InMemoryCoverageStore coverageStore;
    private final RequestIdGenerator requestIdGenerator;

    public ReqoverWebFilter(InMemoryCoverageStore coverageStore, RequestIdGenerator requestIdGenerator) {
        this.coverageStore = Objects.requireNonNull(coverageStore, "coverageStore");
        this.requestIdGenerator = Objects.requireNonNull(requestIdGenerator, "requestIdGenerator");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (exchange.getRequest().getPath().pathWithinApplication().value().startsWith("/reqover/")) {
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
