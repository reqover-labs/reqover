package io.reqover.example.webflux;

import io.reqover.core.ReqoverProbe;
import org.springframework.stereotype.Component;

@Component
public class ReactiveOrderMapper {
    public ReactiveOrderResponse toResponse(long id) {
        ReqoverProbe.hit(ProbeIds.REACTIVE_ORDER_MAPPER, 1);
        return new ReactiveOrderResponse(id, "FOUND");
    }
}

