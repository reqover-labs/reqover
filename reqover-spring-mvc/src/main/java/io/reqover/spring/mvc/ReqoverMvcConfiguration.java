package io.reqover.spring.mvc;

import io.reqover.core.CoverageStore;
import io.reqover.core.InMemoryCoverageStore;
import io.reqover.core.RequestIdGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the Reqover MVC interceptor and its collaborators. Every bean
 * backs off when the application defines its own instance.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ReqoverMvcProperties.class)
public class ReqoverMvcConfiguration {
    @Bean
    @ConditionalOnMissingBean(CoverageStore.class)
    public CoverageStore reqoverCoverageStore(ReqoverMvcProperties properties) {
        return new InMemoryCoverageStore(properties.getMaxSnapshots(), properties.getSnapshotEviction());
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestIdGenerator reqoverRequestIdGenerator() {
        return new RequestIdGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReqoverMvcInterceptor reqoverMvcInterceptor(
            CoverageStore coverageStore,
            RequestIdGenerator requestIdGenerator
    ) {
        return new ReqoverMvcInterceptor(coverageStore, requestIdGenerator);
    }

    @Bean
    public WebMvcConfigurer reqoverWebMvcConfigurer(
            ReqoverMvcInterceptor interceptor,
            ReqoverMvcProperties properties
    ) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor)
                        .addPathPatterns(properties.getIncludePathPatterns())
                        .excludePathPatterns(properties.getExcludePathPatterns());
            }
        };
    }
}
