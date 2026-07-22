package io.reqover.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentOptionsTest {
    @Test
    void parsesIncludeAndExcludePrefixes() {
        AgentOptions options = AgentOptions.parse("include=com.example;org.demo,exclude=com.example.generated");

        assertTrue(options.shouldInstrument("com.example.OrderService"));
        assertTrue(options.shouldInstrument("org.demo.PaymentService"));
        assertFalse(options.shouldInstrument("com.example.generated.GeneratedType"));
        assertFalse(options.shouldInstrument("java.lang.String"));
    }
}

