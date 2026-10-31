package in.codekerdos.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds {@code app.security.public-paths} from application.yml — the list of
 * Ant-style path patterns that {@link in.codekerdos.gateway.security.JwtValidationGatewayFilter}
 * lets through without a JWT check (auth endpoints, actuator health/info,
 * swagger/openapi). Kept out of Java code on purpose so the excluded-path
 * list is a config change, not a redeploy.
 */
@Component
@ConfigurationProperties(prefix = "app.security")
public class GatewaySecurityProperties {

    private List<String> publicPaths = new ArrayList<>();

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }
}
