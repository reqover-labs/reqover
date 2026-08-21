package io.reqover.spring.boot;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/** Opt-in HTTP report endpoint for servlet applications. */
@AutoConfiguration(after = ReqoverReportAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnBean(ReqoverReportService.class)
@ConditionalOnProperty(prefix = "reqover.report.endpoint", name = "enabled", havingValue = "true")
public class ReqoverMvcReportEndpointAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public ReqoverMvcReportEndpoint reqoverMvcReportEndpoint(ReqoverReportService reportService) {
        return new ReqoverMvcReportEndpoint(reportService);
    }
}
