package io.reqover.spring.mvc;

import io.reqover.core.InMemoryCoverageStore;
import io.reqover.core.RequestIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ReqoverMvcConfiguration implements WebMvcConfigurer {
    @Bean
    public InMemoryCoverageStore reqoverCoverageStore() {
        return new InMemoryCoverageStore();
    }

    @Bean
    public RequestIdGenerator reqoverRequestIdGenerator() {
        return new RequestIdGenerator();
    }

    @Bean
    public ReqoverMvcInterceptor reqoverMvcInterceptor(
            InMemoryCoverageStore coverageStore,
            RequestIdGenerator requestIdGenerator
    ) {
        return new ReqoverMvcInterceptor(coverageStore, requestIdGenerator);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(reqoverMvcInterceptor(reqoverCoverageStore(), reqoverRequestIdGenerator()))
                .addPathPatterns("/**")
                .excludePathPatterns("/reqover/**", "/error");
    }
}

