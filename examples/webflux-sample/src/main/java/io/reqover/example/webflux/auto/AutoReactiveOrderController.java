package io.reqover.example.webflux.auto;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class AutoReactiveOrderController {
    private final AutoReactiveOrderService orderService;

    public AutoReactiveOrderController(AutoReactiveOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/auto/reactive/orders/{id}")
    Mono<AutoReactiveOrderResponse> find(@PathVariable long id) {
        return orderService.find(id);
    }
}

