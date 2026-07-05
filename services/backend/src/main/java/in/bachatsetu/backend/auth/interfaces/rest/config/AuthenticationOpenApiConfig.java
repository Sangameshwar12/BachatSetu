package in.bachatsetu.backend.auth.interfaces.rest.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Authentication API metadata exposed through Springdoc. */
@Configuration(proxyBeanMethods = false)
public class AuthenticationOpenApiConfig {

    @Bean
    public OpenAPI bachatSetuOpenApi() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("BachatSetu API")
                        .version("v1")
                        .description("Community savings platform API"));
    }
}
