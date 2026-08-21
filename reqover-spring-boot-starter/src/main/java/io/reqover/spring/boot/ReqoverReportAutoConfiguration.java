package io.reqover.spring.boot;

import io.reqover.core.CoverageStore;
import io.reqover.spring.mvc.ReqoverMvcAutoConfiguration;
import io.reqover.spring.webflux.ReqoverWebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Wires report generation on top of whichever adapter is active.
 *
 * <p>Runs after the MVC and WebFlux adapters so that the {@link CoverageStore}
 * they contribute is already in the context.
 */
@AutoConfiguration(after = {ReqoverMvcAutoConfiguration.class, ReqoverWebFluxAutoConfiguration.class})
@ConditionalOnBean(CoverageStore.class)
@EnableConfigurationProperties(ReqoverReportProperties.class)
public class ReqoverReportAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public ReqoverReportService reqoverReportService(CoverageStore coverageStore) {
        return new ReqoverReportService(coverageStore);
    }

    /**
     * Registered unconditionally because the export is enabled by either of two
     * properties and {@code @ConditionalOnProperty} cannot express that. The
     * bean does nothing when neither path is set.
     */
    @Bean
    @ConditionalOnMissingBean
    public ReqoverReportExporter reqoverReportExporter(
            ReqoverReportService reportService,
            ReqoverReportProperties properties
    ) {
        return new ReqoverReportExporter(reportService, properties.getExport());
    }
}
