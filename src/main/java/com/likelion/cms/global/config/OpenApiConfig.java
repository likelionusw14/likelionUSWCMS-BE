package com.likelion.cms.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI(/swagger-ui.html)와 OpenAPI 문서(/v3/api-docs)를 제공한다.
 *
 * <p>인증이 필요한 API는 HS256 Bearer 액세스 토큰을 쓴다. 아래 SecurityScheme 로
 * Swagger UI 의 "Authorize" 버튼에 토큰을 넣고 호출을 테스트할 수 있게 한다.
 * 쿠키 기반 흐름(refresh/CSRF)은 Swagger UI 로 다루기 까다로워 Bearer 만 문서화한다.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("likelion USW CMS API")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
