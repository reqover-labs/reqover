package io.reqover.example.webflux;

import io.reqover.core.ReqoverProbe;
import org.springframework.stereotype.Component;

@Component
public class ReactiveValidator {
    public void validate(String value) {
        ReqoverProbe.hit(ProbeIds.REACTIVE_VALIDATOR, 1);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}

