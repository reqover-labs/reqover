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

    @Test
    void defaultExcludesDoNotBlockReqoverExamples() {
        AgentOptions options = AgentOptions.parse("include=io.reqover.example");

        assertTrue(options.shouldInstrument("io.reqover.example.mvc.auto.AutoOrderService"));
        assertFalse(options.shouldInstrument("io.reqover.core.ReqoverProbe"));
        assertFalse(options.shouldInstrument("io.reqover.agent.ReqoverAgent"));
    }

    @Test
    void explicitIncludeOverridesShorterDefaultExclude() {
        AgentOptions options = AgentOptions.parse("include=org.springframework.samples.");

        assertTrue(options.shouldInstrument("org.springframework.samples.PetClinicService"));
        assertFalse(options.shouldInstrument("org.springframework.web.servlet.DispatcherServlet"));
    }

    @Test
    void hardExcludesCannotBeOverriddenByMoreSpecificIncludes() {
        AgentOptions options = AgentOptions.parse(String.join(";",
                "include=java.lang.",
                "io.reqover.core.Reqover",
                "io.reqover.agent.internal.asm.",
                "io.reqover.instrumentation.Reqover",
                "io.reqover.report.Coverage",
                "io.reqover.spring.webflux.Reqover",
                "org.objectweb.asm.tree."
        ));

        assertFalse(options.shouldInstrument("java.lang.String"));
        assertFalse(options.shouldInstrument("io.reqover.core.ReqoverProbe"));
        assertFalse(options.shouldInstrument("io.reqover.agent.internal.asm.ClassReader"));
        assertFalse(options.shouldInstrument("io.reqover.instrumentation.ReqoverClassInstrumenter"));
        assertFalse(options.shouldInstrument("io.reqover.report.CoverageReportGenerator"));
        assertFalse(options.shouldInstrument("io.reqover.spring.webflux.ReqoverWebFilter"));
        assertFalse(options.shouldInstrument("org.objectweb.asm.tree.ClassNode"));
    }

    @Test
    void userExcludeOverridesShorterInclude() {
        AgentOptions options = AgentOptions.parse("include=com.example,exclude=com.example.internal.");

        assertTrue(options.shouldInstrument("com.example.OrderService"));
        assertFalse(options.shouldInstrument("com.example.internal.Hidden"));
    }

    @Test
    void ignoresUnknownAndMalformedOptions() {
        AgentOptions options = AgentOptions.parse("includes=com.typo,verbose,include=com.example");

        assertTrue(options.shouldInstrument("com.example.OrderService"));
        assertFalse(options.shouldInstrument("com.typo.NotIncluded"));
    }

    @Test
    void failsClosedWithoutAValidInclude() {
        assertFalse(AgentOptions.parse(null).shouldInstrument("com.example.OrderService"));
        assertFalse(AgentOptions.parse("   ").shouldInstrument("com.example.OrderService"));
        assertFalse(AgentOptions.parse("verbose,exclude=com.example.generated")
                .shouldInstrument("com.example.OrderService"));
    }
}
