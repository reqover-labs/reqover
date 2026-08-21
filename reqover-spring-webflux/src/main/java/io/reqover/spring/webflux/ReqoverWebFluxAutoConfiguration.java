package io.reqover.spring.webflux;

import io.micrometer.context.ContextRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Hooks;

/**
 * Auto-configuration entry point for reactive applications using WebFlux.
 *
 * <p>The class conditions matter for the starter: a servlet application that
 * depends on {@code reqover-spring-boot-starter} has this adapter on the
 * classpath but neither Reactor nor WebFlux, and must not fail because of it.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass({WebFilter.class, Hooks.class, ContextRegistry.class})
@ConditionalOnProperty(prefix = "reqover.webflux", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(ReqoverWebFluxConfiguration.class)
public class ReqoverWebFluxAutoConfiguration {
}
