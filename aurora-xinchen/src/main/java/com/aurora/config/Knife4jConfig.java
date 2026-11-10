package com.aurora.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 文档生成配置（knife4j 4.x / springdoc OpenAPI 3）
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI auroraOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("aurora文档")
                        .description("aurora")
                        .contact(new Contact().name("xinchenXE").email("1785148923@qq.com"))
                        .version("2.0"));
    }

    @Bean
    public GroupedOpenApi controllerApi() {
        return GroupedOpenApi.builder()
                .group("aurora")
                .packagesToScan("com.aurora.controller")
                .build();
    }
}
