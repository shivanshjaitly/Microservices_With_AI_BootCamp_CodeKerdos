package in.codekerdos.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test — verifies the gateway's reactive context loads cleanly.
 * Eureka client registration is not disabled here; in CI without a running
 * Eureka server this simply logs retry warnings and still starts (the
 * eureka-client autoconfiguration does not fail context startup when the
 * registry is unreachable).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
