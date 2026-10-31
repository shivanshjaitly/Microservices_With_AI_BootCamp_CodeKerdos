package in.codekerdos.gateway.security;

import in.codekerdos.gateway.config.GatewaySecurityProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Edge JWT guard. Runs before routing on every request; requests matching one
 * of {@code app.security.public-paths} (auth endpoints, actuator health/info,
 * swagger) pass through untouched. Everything else must carry a valid
 * {@code Authorization: Bearer <token>} header or gets a 401 here — before
 * the request is even proxied downstream.
 * <p>
 * This is a fast-fail convenience, not the only line of defense: every
 * downstream business service also runs its own {@code JwtAuthFilter} and
 * validates independently (see docs/PROJECT2_SPEC.md), since services may be
 * called directly (service-to-service, tests) without going through the
 * gateway.
 */
@Component
public class JwtValidationGatewayFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtService jwtService;
    private final GatewaySecurityProperties securityProperties;

    public JwtValidationGatewayFilter(JwtService jwtService, GatewaySecurityProperties securityProperties) {
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or malformed Authorization header");
        }

        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            return unauthorized(exchange, "Invalid or expired JWT");
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1; // run early, ahead of the routing/load-balancer filters
    }

    private boolean isPublicPath(String path) {
        return securityProperties.getPublicPaths().stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"timestamp\":\"%s\",\"status\":401,\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now(), message, exchange.getRequest().getURI().getPath());
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
