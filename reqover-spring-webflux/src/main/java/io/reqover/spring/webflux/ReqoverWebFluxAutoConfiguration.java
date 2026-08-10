package io.reqover.spring.webflux;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(prefix = "reqover.webflux", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(ReqoverWebFluxConfiguration.class)
public class ReqoverWebFluxAutoConfiguration {
}
