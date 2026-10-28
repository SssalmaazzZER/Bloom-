package ma.bloome.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bloomeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bloomé API")
                        .version("v1")
                        .description("""
                                REST API of the Bloomé flower boutique.

                                **Authentication**: session cookie `BLOOME_SESSION` (HttpOnly) obtained via
                                `POST /api/v1/auth/login`. **CSRF**: every state-changing request must send the
                                value of the `XSRF-TOKEN` cookie in the `X-XSRF-TOKEN` header (call
                                `GET /api/v1/auth/csrf` first to receive the cookie).
                                Prices are in Moroccan dirhams (MAD) and always computed server-side."""))
                .components(new Components()
                        .addSecuritySchemes("session", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("BLOOME_SESSION")));
    }
}
