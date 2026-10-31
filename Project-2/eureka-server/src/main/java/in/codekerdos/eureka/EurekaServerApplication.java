package in.codekerdos.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Standalone Eureka service registry for Project 2 (Order Platform with AI
 * Risk Scoring). Every business service and the API gateway register here so
 * the gateway can route by Eureka application name (lb://order-service, ...)
 * instead of hardcoded host:port pairs. This server registers nowhere and
 * fetches nothing itself — see application.yml.
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
