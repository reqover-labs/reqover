package io.reqover.spring.boot;

import io.reqover.spring.mvc.ReqoverMvcAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReqoverReportAutoConfigurationTest {
    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                    ReqoverMvcAutoConfiguration.class,
                    ReqoverReportAutoConfiguration.class,
                    ReqoverMvcReportEndpointAutoConfiguration.class
            ));

    @Test
    void buildsReportsOnTopOfTheActiveAdapter() {
        contextRunner.run(context -> assertEquals(1, context.getBeansOfType(ReqoverReportService.class).size()));
    }

    @Test
    void keepsTheHttpEndpointOffUnlessItIsAskedFor() {
        contextRunner.run(context ->
                assertTrue(context.getBeansOfType(ReqoverMvcReportEndpoint.class).isEmpty(),
                        "the report names internal classes, so it must not be exposed by default"));
    }

    @Test
    void registersTheHttpEndpointWhenEnabled() {
        contextRunner
                .withPropertyValues("reqover.report.endpoint.enabled=true")
                .run(context -> assertEquals(1, context.getBeansOfType(ReqoverMvcReportEndpoint.class).size()));
    }

    @Test
    void staysOutOfTheContextWhenNoAdapterContributedAStore() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ReqoverReportAutoConfiguration.class))
                .run(context -> assertTrue(context.getBeansOfType(ReqoverReportService.class).isEmpty()));
    }

    @Test
    void alwaysRegistersTheExporterBecauseTwoPropertiesCanEnableIt() {
        contextRunner.run(context -> assertEquals(1, context.getBeansOfType(ReqoverReportExporter.class).size()));
    }

    @Test
    void bindsTheExportPaths() {
        contextRunner
                .withPropertyValues(
                        "reqover.report.export.json-path=build/report.json",
                        "reqover.report.export.html-path=build/report.html"
                )
                .run(context -> {
                    ReqoverReportProperties properties = context.getBean(ReqoverReportProperties.class);
                    assertEquals("build/report.json", properties.getExport().getJsonPath());
                    assertEquals("build/report.html", properties.getExport().getHtmlPath());
                    assertTrue(properties.getExport().isEnabled());
                });
    }

    @Test
    void treatsAnUnsetExportAsDisabled() {
        contextRunner.run(context ->
                assertTrue(!context.getBean(ReqoverReportProperties.class).getExport().isEnabled()));
    }
}
