package io.reqover.example.mvc.order;

import io.reqover.core.ReqoverProbe;
import io.reqover.example.mvc.ProbeIds;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders/{id}")
    OrderResponse find(@PathVariable long id) {
        ReqoverProbe.hit(ProbeIds.ORDER_CONTROLLER, 1);
        return orderService.find(id);
    }
}

