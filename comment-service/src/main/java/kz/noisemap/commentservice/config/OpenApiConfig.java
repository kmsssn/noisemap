package kz.noisemap.commentservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI commentServiceOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("/").description("Default (current host)")
                ))
                .info(new Info()
                        .title("NoiseMap — Comment Service API")
                        .description("Комментарии пользователей на карте шума. "
                                + "Пользователи оставляют комментарии с координатами, "
                                + "они отображаются на тепловой карте.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Token",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}