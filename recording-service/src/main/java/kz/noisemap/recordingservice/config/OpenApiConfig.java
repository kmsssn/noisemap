package kz.noisemap.recordingservice.config;

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
    public OpenAPI recordingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NoiseMap — Recording Service API")
                        .description("Приём аудиозаписей шума от пользователей. "
                                + "Валидация, сохранение, публикация событий для ML-классификации. "
                                + "Загрузка через multipart/form-data.")
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
