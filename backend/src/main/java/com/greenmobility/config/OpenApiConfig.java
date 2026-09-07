package com.greenmobility.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "BearerAuth";

        return new OpenAPI()
                .servers(List.of(
                        new Server().url(contextPath).description("Current Environment Server")
                ))
                .info(new Info()
                        .title("Green Mobility Platform API")
                        .description("Tài liệu đặc tả API chuẩn OpenAPI 3.0 / Swagger cho Nền tảng Gọi xe & Giao hàng Xanh thông minh.\n\n" +
                                "Hệ thống hỗ trợ tính toán giảm phát thải khí nhà kính CO2 (IPCC/MoNRE), quản lý xe điện (EV), " +
                                "và xác thực khuôn mặt sinh trắc học khi tài xế vào ca làm việc.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Green Mobility Team")
                                .email("dev@greenmobility.vn"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Nhập Bearer JWT Token được sinh ra từ API `/auth/login` hoặc `/auth/register`")));
    }
}
