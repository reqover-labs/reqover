package io.reqover.example.mvc;

import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProbeMetadataConfiguration {
    @PostConstruct
    void registerMetadata() {
        ProbeRegistry.register(new ProbeMetadata(ProbeIds.ORDER_CONTROLLER, 1, "OrderController", "find", "(long)", null));
        ProbeRegistry.register(new ProbeMetadata(ProbeIds.ORDER_SERVICE, 1, "OrderService", "find", "(long)", null));
        ProbeRegistry.register(new ProbeMetadata(ProbeIds.PAYMENT_CONTROLLER, 1, "PaymentController", "pay", "()", null));
        ProbeRegistry.register(new ProbeMetadata(ProbeIds.PAYMENT_SERVICE, 1, "PaymentService", "pay", "()", null));
        ProbeRegistry.register(new ProbeMetadata(ProbeIds.SHARED_VALIDATOR, 1, "SharedValidator", "validate", "(String)", null));
    }
}

