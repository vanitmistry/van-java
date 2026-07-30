package com.example.aws.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AWS Shopping Cart API")
                        .version("1.0.0")
                        .description("DynamoDB-backed shopping cart with inventory management, " +
                                "partial fills, and atomic transactions. Includes S3 storage and SQS messaging.")
                );
    }
}
