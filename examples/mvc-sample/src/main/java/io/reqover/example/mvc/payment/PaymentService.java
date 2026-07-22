package io.reqover.example.mvc.payment;

import io.reqover.core.ReqoverProbe;
import io.reqover.example.mvc.ProbeIds;
import io.reqover.example.mvc.SharedValidator;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private final SharedValidator validator;

    public PaymentService(SharedValidator validator) {
        this.validator = validator;
    }

    public PaymentResponse pay() {
        ReqoverProbe.hit(ProbeIds.PAYMENT_SERVICE, 1);
        validator.validate("payment");
        return new PaymentResponse("APPROVED");
    }
}

