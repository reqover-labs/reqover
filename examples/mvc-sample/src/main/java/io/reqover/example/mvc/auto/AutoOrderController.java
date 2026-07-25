package io.reqover.example.mvc.auto;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AutoOrderController {
    private final AutoOrderService orderService;

    public AutoOrderController(AutoOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/auto/orders/{id}")
    AutoOrderResponse find(@PathVariable long id) {
        return orderService.find(id);
    }
}

