package com.notfound.newsservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String HEADER_USER_ID = "userIdHeader";
    public static final String HEADER_USER_ROLE = "userRoleHeader";
    public static final String HEADER_USER_NAME = "userNameHeader";

    @Bean
    public OpenAPI newsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bookstore News Service API")
                        .version("1.0")
                        .description("""
                                API tin tức (`/api/v1/news`). **GET** công khai không cần header. Thao tác **Admin** cần \
                                `X-User-Role: ROLE_ADMIN`; một số API cần thêm `X-User-Id` (UUID) và tùy chọn \
                                `X-User-Name` (tên hiển thị). Dùng **Authorize** trên Swagger UI để thử header."""))
                .components(new Components()
                        .addSecuritySchemes(HEADER_USER_ID, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-User-Id")
                                .description("UUID tác giả / người dùng"))
                        .addSecuritySchemes(HEADER_USER_ROLE, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-User-Role")
                                .description("ROLE_ADMIN cho CRUD / publish / upload ảnh"))
                        .addSecuritySchemes(HEADER_USER_NAME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-User-Name")
                                .description("Tên hiển thị khi tạo tin (optional)")));
    }
}
