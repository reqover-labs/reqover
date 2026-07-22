package io.reqover.example.mvc.order;

import io.reqover.core.ReqoverProbe;
import io.reqover.example.mvc.ProbeIds;
import io.reqover.example.mvc.SharedValidator;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final SharedValidator validator;

    public OrderService(SharedValidator validator) {
        this.validator = validator;
    }

    public OrderResponse find(long id) {
        ReqoverProbe.hit(ProbeIds.ORDER_SERVICE, 1);
        validator.validate("order-" + id);
        return new OrderResponse(id, "FOUND");
    }
}

