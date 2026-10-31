package in.codekerdos.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Reactive (WebFlux) edge gateway for Project 2 (Order Platform with AI Risk
 * Scoring). Routes /api/** requests to the right business service by Eureka
 * application name (lb://order-service, ...) and fast-fails requests with a
 * missing/invalid JWT before they are even routed — see
 * {@link in.codekerdos.gateway.security.JwtValidationGatewayFilter}.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
