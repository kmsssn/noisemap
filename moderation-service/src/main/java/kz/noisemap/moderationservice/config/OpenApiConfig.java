package kz.noisemap.moderationservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI moderationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NoiseMap — Moderation Service API")
                        .description("Модерация данных. Автоматическое обнаружение аномалий, "
                                + "очередь ручной модерации, решения по записям. "
                                + "Доступ: роли MODERATOR и ADMIN.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("NoiseMap Team")
                                .email("noisemap@example.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Token",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
