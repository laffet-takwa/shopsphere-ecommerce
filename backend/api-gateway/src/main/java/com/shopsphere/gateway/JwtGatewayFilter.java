package com.shopsphere.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {
    private final SecretKey key;

    public JwtGatewayFilter(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();
        if (method == HttpMethod.OPTIONS) return chain.filter(exchange);
        if ((path.equals("/api/auth/register") || path.equals("/api/auth/login")) && method == HttpMethod.POST) {
            return chain.filter(exchange);
        }
        if (path.startsWith("/api/products/") || path.equals("/api/products")) {
            if (method == HttpMethod.GET) return chain.filter(exchange);
        }

        String authorization = request.getHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) return reject(exchange, HttpStatus.UNAUTHORIZED);
        try {
            var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(authorization.substring(7)).getPayload();
            String role = claims.get("role", String.class);
            Number userIdClaim = claims.get("userId", Number.class);
            if (userIdClaim == null || role == null) return reject(exchange, HttpStatus.UNAUTHORIZED);
            String userId = Long.toString(userIdClaim.longValue());
            boolean write = method != HttpMethod.GET && method != HttpMethod.HEAD && method != HttpMethod.OPTIONS;
            boolean orderShipping = path.startsWith("/api/orders/") && path.endsWith("/ship");
            boolean adminWrite = write && (path.startsWith("/api/products")
                    || path.startsWith("/api/inventory")
                    || orderShipping);
            if (adminWrite && !"ADMIN".equals(role)) return reject(exchange, HttpStatus.FORBIDDEN);
            ServerHttpRequest authenticated = request.mutate()
                    .headers(headers -> {
                        headers.remove("X-User-Id");
                        headers.remove("X-User-Role");
                        headers.set("X-User-Id", userId);
                        headers.set("X-User-Role", role);
                    }).build();
            return chain.filter(exchange.mutate().request(authenticated).build());
        } catch (RuntimeException exception) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() { return -100; }
}