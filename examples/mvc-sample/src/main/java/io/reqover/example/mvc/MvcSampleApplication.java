package io.reqover.example.mvc;

import io.reqover.spring.mvc.ReqoverMvcConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(ReqoverMvcConfiguration.class)
public class MvcSampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(MvcSampleApplication.class, args);
    }
}

