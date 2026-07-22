package io.reqover.example.webflux;

import io.reqover.spring.webflux.ReqoverWebFluxConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(ReqoverWebFluxConfiguration.class)
public class WebFluxSampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebFluxSampleApplication.class, args);
    }
}

