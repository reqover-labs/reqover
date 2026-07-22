package io.reqover.example.webflux;

import io.reqover.core.ReqoverProbe;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class ReactiveOrderController {
    private final ReactiveOrderService orderService;

    public ReactiveOrderController(ReactiveOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/reactive/orders/{id}")
    Mono<ReactiveOrderResponse> find(@PathVariable long id) {
        return Mono.defer(() -> {
            ReqoverProbe.hit(ProbeIds.REACTIVE_ORDER_CONTROLLER, 1);
            return orderService.find(id);
        });
    }
}

