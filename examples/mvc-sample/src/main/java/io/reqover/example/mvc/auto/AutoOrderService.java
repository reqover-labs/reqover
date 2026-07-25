package io.reqover.example.mvc.auto;

import org.springframework.stereotype.Service;

@Service
public class AutoOrderService {
    public AutoOrderResponse find(long id) {
        validate(id);
        return new AutoOrderResponse(id, "AUTO_FOUND");
    }

    private void validate(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
    }
}

