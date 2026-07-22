package io.reqover.example.webflux;

import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProbeMetadataConfiguration {
    @PostConstruct
    void registerMetadata() {
        ProbeRegistry.register(new ProbeMetadata(ProbeIds.REACTIVE_ORDER_CONTROLLER, 1, "ReactiveOrderController", "find", "(long)", null));
        ProbeRegistry.register(new ProbeMetadata(ProbeIds.REACTIVE_ORDER_SERVICE, 1, "ReactiveOrderService", "find", "(long)", null));
        ProbeRegistry.register(new ProbeMetadata(ProbeIds.REACTIVE_ORDER_MAPPER, 1, "ReactiveOrderMapper", "toResponse", "(long)", null));
        ProbeRegistry.register(new ProbeMetadata(ProbeIds.REACTIVE_VALIDATOR, 1, "ReactiveValidator", "validate", "(String)", null));
    }
}

