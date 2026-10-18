package in.coderkerdos.ticketservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CodeKerdos Ticket Service")
                        .description("Project #1 — AI Support Ticket Router: Ticket CRUD + AI-classified triage")
                        .version("1.0"));
    }
}