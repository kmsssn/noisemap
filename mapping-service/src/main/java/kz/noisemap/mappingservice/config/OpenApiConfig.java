package kz.noisemap.mappingservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mappingServiceOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("/").description("Default (current host)")
                ))
                .info(new Info()
                        .title("NoiseMap — Mapping Service API")
                        .description("Тепловая карта шумового загрязнения. "
                                + "Получение тайлов по bounding box, поиск рядом с точкой. "
                                + "Данные агрегируются из классифицированных измерений.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("NoiseMap Team")
                                .email("noisemap@example.com")));
    }
}