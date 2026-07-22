package io.reqover.example.mvc.payment;

import io.reqover.core.ReqoverProbe;
import io.reqover.example.mvc.ProbeIds;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments")
    PaymentResponse pay() {
        ReqoverProbe.hit(ProbeIds.PAYMENT_CONTROLLER, 1);
        return paymentService.pay();
    }
}

