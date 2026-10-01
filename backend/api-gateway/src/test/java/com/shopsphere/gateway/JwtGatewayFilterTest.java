package com.shopsphere.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * The gateway is the only place identity is established, so these tests pin the rules downstream
 * services depend on: which paths are public, and which writes are ADMIN-only.
 */
class JwtGatewayFilterTest {

    private static final String SECRET = "ZGV2LW9ubHktc2VjcmV0LWtleS1tdXN0LWJlLWNoYW5nZWQtMzItYnl0ZXM=";

    private static String token(long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("userId", userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private static MockServerWebExchange authenticated(MockServerHttpRequest.BaseBuilder<?> request,
                                                       long userId, String role) {
        return MockServerWebExchange.from(
                request.header("Authorization", "Bearer " + token(userId, role)).build());
    }

    private static boolean continued(JwtGatewayFilter filter, MockServerWebExchange exchange) {
        AtomicBoolean proceeded = new AtomicBoolean();
        filter.filter(exchange, ignored -> {
            proceeded.set(true);
            return Mono.empty();
        }).block();
        return proceeded.get();
    }

    @Test
    void allowsPublicCatalogReads() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products").build());

        assertTrue(continued(new JwtGatewayFilter(SECRET), exchange));
    }

    @Test
    void allowsPublicRegistration() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/register").build());

        assertTrue(continued(new JwtGatewayFilter(SECRET), exchange));
    }

    @Test
    void rejectsUnauthenticatedOrderRequests() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders").build());

        new JwtGatewayFilter(SECRET).filter(exchange, ignored -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void rejectsCustomerCatalogWrites() {
        var exchange = authenticated(MockServerHttpRequest.post("/api/products"), 7L, "CUSTOMER");

        new JwtGatewayFilter(SECRET).filter(exchange, ignored -> Mono.empty()).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void rejectsCustomerOrderCancellationByShipPath() {
        var exchange = authenticated(MockServerHttpRequest.put("/api/orders/12/ship"), 7L, "CUSTOMER");

        new JwtGatewayFilter(SECRET).filter(exchange, ignored -> Mono.empty()).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void allowsAdminOrderShipping() {
        var exchange = authenticated(MockServerHttpRequest.put("/api/orders/12/ship"), 1L, "ADMIN");

        assertTrue(continued(new JwtGatewayFilter(SECRET), exchange));
    }

    @Test
    void stillAllowsCustomersToCreateOrders() {
        var exchange = authenticated(MockServerHttpRequest.post("/api/orders"), 7L, "CUSTOMER");

        assertTrue(continued(new JwtGatewayFilter(SECRET), exchange));
    }

    @Test
    void overwritesSpoofedIdentityHeaders() {
        var exchange = authenticated(
                MockServerHttpRequest.get("/api/orders").header("X-User-Id", "999").header("X-User-Role", "ADMIN"),
                7L, "CUSTOMER");
        AtomicReference<String> forwardedId = new AtomicReference<>();
        AtomicReference<String> forwardedRole = new AtomicReference<>();

        new JwtGatewayFilter(SECRET).filter(exchange, forwarded -> {
            forwardedId.set(forwarded.getRequest().getHeaders().getFirst("X-User-Id"));
            forwardedRole.set(forwarded.getRequest().getHeaders().getFirst("X-User-Role"));
            return Mono.empty();
        }).block();

        assertThat(forwardedId.get()).isEqualTo("7");
        assertThat(forwardedRole.get()).isEqualTo("CUSTOMER");
    }
}