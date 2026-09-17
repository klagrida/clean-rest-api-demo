package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
            .info(new Info()
                .title("Clean REST API Demo")
                .description("Resource-based endpoints with service-layer exception handling, "
                    + "mapped to RFC 7807 Problem Details responses.")
                .version("v1")
                .contact(new Contact().name("Demo").email("demo@example.com")));
    }
}
