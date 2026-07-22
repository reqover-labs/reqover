package io.reqover.example.mvc;

import io.reqover.core.ReqoverProbe;
import org.springframework.stereotype.Component;

@Component
public class SharedValidator {
    public void validate(String value) {
        ReqoverProbe.hit(ProbeIds.SHARED_VALIDATOR, 1);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}

