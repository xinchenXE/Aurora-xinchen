package com.aurora;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;


@RefreshScope
@EnableDiscoveryClient
@SpringBootApplication
@MapperScan("com.aurora.mapper")
public class AuroraXinchenApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuroraXinchenApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
