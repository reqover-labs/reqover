package io.reqover.spring.webflux;

import io.micrometer.context.ContextRegistry;
import io.reqover.core.CoverageStore;
import io.reqover.core.InMemoryCoverageStore;
import io.reqover.core.RequestIdGenerator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Registers the Reqover WebFlux filter and its collaborators.
 *
 * <p>Initialization installs a JVM-wide {@code ThreadLocalAccessor} and
 * enables Reactor's automatic context propagation via
 * {@link Hooks#enableAutomaticContextPropagation()}. The hook changes Reactor
 * behavior for the whole application, which is what lets coverage buckets
 * follow a request across scheduler thread hops. It is installed once, even
 * if the application context is refreshed.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ReqoverWebFluxProperties.class)
public class ReqoverWebFluxConfiguration implements InitializingBean {
    private static final AtomicBoolean CONTEXT_PROPAGATION_INSTALLED = new AtomicBoolean();

    @Override
    public void afterPropertiesSet() {
        if (CONTEXT_PROPAGATION_INSTALLED.compareAndSet(false, true)) {
            ContextRegistry.getInstance().registerThreadLocalAccessor(new ReqoverThreadLocalAccessor());
            Hooks.enableAutomaticContextPropagation();
        }
    }

    @Bean
    @ConditionalOnMissingBean(CoverageStore.class)
    public CoverageStore reqoverCoverageStore(ReqoverWebFluxProperties properties) {
        return new InMemoryCoverageStore(properties.getMaxSnapshots());
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestIdGenerator reqoverRequestIdGenerator() {
        return new RequestIdGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReqoverWebFilter reqoverWebFilter(
            CoverageStore coverageStore,
            RequestIdGenerator requestIdGenerator,
            ReqoverWebFluxProperties properties
    ) {
        return new ReqoverWebFilter(coverageStore, requestIdGenerator, properties.getExcludePathPrefixes());
    }
}
