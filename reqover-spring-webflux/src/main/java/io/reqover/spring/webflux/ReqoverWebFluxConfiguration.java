package io.reqover.spring.webflux;

import io.micrometer.context.ContextRegistry;
import io.reqover.core.InMemoryCoverageStore;
import io.reqover.core.RequestIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

@Configuration
public class ReqoverWebFluxConfiguration {
    public ReqoverWebFluxConfiguration() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(new ReqoverThreadLocalAccessor());
        Hooks.enableAutomaticContextPropagation();
    }

    @Bean
    public InMemoryCoverageStore reqoverCoverageStore() {
        return new InMemoryCoverageStore();
    }

    @Bean
    public RequestIdGenerator reqoverRequestIdGenerator() {
        return new RequestIdGenerator();
    }

    @Bean
    public ReqoverWebFilter reqoverWebFilter(
            InMemoryCoverageStore coverageStore,
            RequestIdGenerator requestIdGenerator
    ) {
        return new ReqoverWebFilter(coverageStore, requestIdGenerator);
    }
}

